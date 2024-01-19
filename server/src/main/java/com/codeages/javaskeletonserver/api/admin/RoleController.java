package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.user.service.RoleService;
import com.codeages.javaskeletonserver.biz.user.dto.RoleDto;
import com.codeages.javaskeletonserver.biz.user.dto.RoleSearchParams;
import com.codeages.javaskeletonserver.biz.user.dto.RoleCreateParams;
import com.codeages.javaskeletonserver.biz.user.dto.RoleUpdateParams;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;

import javax.annotation.security.RolesAllowed;
import java.util.List;

@RestController
@RequestMapping("/api-admin/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/search")
    public PagerResponse<RoleDto> search(RoleSearchParams searchParams,
                                         @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                roleService.search(searchParams, pager),
                pager
        );
    }

    @GetMapping("/list")
    public List<RoleDto> list(RoleSearchParams searchParams) {
        return roleService.search(searchParams, Pageable.unpaged()).getContent();
    }

    @PostMapping("/create")

    public OkResponse create(@RequestBody RoleCreateParams createParams) {
        roleService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")

    public OkResponse update(@RequestBody RoleUpdateParams updateParams) {
        roleService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")

    public OkResponse delete(@RequestBody IdPayload idPayload) {
        roleService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


