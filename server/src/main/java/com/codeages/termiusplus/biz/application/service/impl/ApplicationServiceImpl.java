package com.codeages.termiusplus.biz.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.application.dto.*;
import com.codeages.termiusplus.biz.application.entity.Application;
import com.codeages.termiusplus.biz.application.mapper.ApplicationMapper;
import com.codeages.termiusplus.biz.application.repository.ApplicationRepository;
import com.codeages.termiusplus.biz.application.service.ApplicationMonitorService;
import com.codeages.termiusplus.biz.application.service.ApplicationServerService;
import com.codeages.termiusplus.biz.application.service.ApplicationService;
import com.codeages.termiusplus.biz.server.dto.TreeSortParams;
import com.codeages.termiusplus.biz.util.TreeUtils;
import com.codeages.termiusplus.exception.AppException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final ApplicationMapper applicationMapper;

    private final Validator validator;

    private final ApplicationMonitorService applicationMonitorService;

    private final ApplicationServerService applicationServerService;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  ApplicationMapper applicationMapper,
                                  Validator validator, ApplicationMonitorService applicationMonitorService,
                                  ApplicationServerService applicationServerService) {
        this.applicationRepository = applicationRepository;
        this.applicationMapper = applicationMapper;
        this.validator = validator;
        this.applicationMonitorService = applicationMonitorService;
        this.applicationServerService = applicationServerService;
    }

    @Override
    public List<Tree<Long>> findAll() {
        List<Application> applications = applicationRepository.findAll();

        List<TreeNode<Long>> servers = applications
                .stream()
                .map(e -> {
                    TreeNode<Long> longTreeNode = new TreeNode<>(
                            e.getId(),
                            e.getParentId(),
                            e.getName(),
                            e.getSort()
                    );
                    Map<String, Object> beanMap = BeanUtil.beanToMap(e);

                    longTreeNode.setExtra(beanMap);

                    return longTreeNode;
                })
                .collect(Collectors.toList());

        return TreeUtil.build(servers, 0L);
    }

    @Override
    public List<ApplicationDto> findAllApplication() {
        return applicationRepository.findAllByIsGroupFalse()
                                    .stream()
                                    .map(applicationMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void create(ApplicationCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        Application application = applicationRepository.save(applicationMapper.toCreateEntity(createParams));

        if (createParams.getMonitorType() != null && createParams.getMonitorConfig()!= null && Boolean.FALSE.equals(createParams.getIsGroup())) {
            applicationMonitorService.create(new ApplicationMonitorCreateParams(
                    application.getId(),
                    createParams.getMonitorType(),
                    createParams.getMonitorConfig(),
                    createParams.getRemark()
            ));
        }

        if (CollectionUtil.isNotEmpty(createParams.getServerList())) {
            createParams.getServerList().forEach(e -> {
                e.setApplicationId(application.getId());
            });

            createParams.getServerList().forEach(applicationServerService::create);
        }
    }

    @Override
    @Transactional
    public void update(ApplicationUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var application = applicationRepository.findById(updateParams.getId())
                                               .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        applicationMapper.toUpdateEntity(application, updateParams);
        application.setProxyId(updateParams.getProxyId());
        applicationRepository.save(application);

        if (updateParams.getMonitorType() != null) {
            applicationMonitorService.getByApplicationId(application.getId()).ifPresentOrElse(monitor -> {
                applicationMonitorService.update(new ApplicationMonitorUpdateParams(
                        monitor.getId(),
                        updateParams.getId(),
                        updateParams.getMonitorType(),
                        updateParams.getMonitorConfig(),
                        updateParams.getRemark(),
                        monitor.getFailureCount(),
                        monitor.getFailureTime(),
                        monitor.getResponseResult(),
                        monitor.getResponseTime()
                ));
            },

                    () -> applicationMonitorService.create(new ApplicationMonitorCreateParams(
                            application.getId(),
                            updateParams.getMonitorType(),
                            updateParams.getMonitorConfig(),
                            updateParams.getRemark()
                    ))
            );
        } else {
            applicationMonitorService.deleteByApplicationId(application.getId());
        }

        if (CollectionUtil.isNotEmpty(updateParams.getServerList())) {
            applicationServerService.deleteByApplicationId(application.getId());
            updateParams.getServerList().forEach(e -> e.setApplicationId(application.getId()));
            updateParams.getServerList().forEach(applicationServerService::create);
        } else {
            applicationServerService.deleteByApplicationId(application.getId());
        }

    }

    @Override
    @Transactional
    public void delete(Long id) {
        List<Application> children = applicationRepository.findAllByParentId(id);
        if (CollUtil.isNotEmpty(children)) {
            children.stream().map(Application::getId).forEach(this::delete);
        }

        Application application = applicationRepository.findById(id)
                                                       .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        applicationRepository.delete(application);

        applicationMonitorService.deleteByApplicationId(id);
    }

    @Override
    @Transactional
    public void sort(List<TreeSortParams> treeSortParams) {
        for (int i = 0; i < treeSortParams.size(); i++) {
            treeSortParams.get(i).setSort((long) i);
            TreeUtils.rebuildSeq(treeSortParams.get(i));
        }

        applicationRepository.saveAll(toUpdateAllEntity(treeSortParams));
    }

    @Override
    public List<ApplicationDto> findAllByIds(Collection<Long> ids) {
        return applicationRepository.findAllById(ids)
                                     .stream()
                                     .map(applicationMapper::toDto)
                                     .collect(Collectors.toList());
    }

    @Override
    public List<Tree<Long>> groupList() {
        List<TreeNode<Long>> servers = applicationRepository.findAllByIsGroupTrue()
                                                       .stream()
                                                       .map(e -> {
                                                           TreeNode<Long> longTreeNode = new TreeNode<>(
                                                                   e.getId(),
                                                                   e.getParentId(),
                                                                   e.getName(),
                                                                   e.getSort()
                                                           );
                                                           longTreeNode.setExtra(BeanUtil.beanToMap(e));
                                                           return longTreeNode;
                                                       })
                                                       .collect(Collectors.toList());

        Tree<Long> root = new Tree<>();
        root.setId(0L);
        root.setName("all");
        root.setChildren(TreeUtil.build(servers, 0L));
        return List.of(root);
    }

    @Override
    public boolean existByProxyId(Long proxyId) {
        return applicationRepository.existsByProxyId(proxyId);
    }

    private List<Application> toUpdateAllEntity(List<TreeSortParams> serverUpdateParams) {
        if (CollUtil.isEmpty(serverUpdateParams)) {
            return Collections.emptyList();
        }

        List<Application> servers = new ArrayList<>();

        for (TreeSortParams serverUpdateParam : serverUpdateParams) {
            servers.add(
                    applicationMapper.toUpdateAllEntity(
                            applicationRepository.findById(serverUpdateParam.getId())
                                                 .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND)),
                            serverUpdateParam
                    )
            );
            servers.addAll(toUpdateAllEntity(serverUpdateParam.getChildren()));
        }

        return servers;
    }
}


