package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.server.dto.GroupCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.GroupDto;
import com.codeages.javaskeletonserver.biz.server.dto.GroupSearchParams;
import com.codeages.javaskeletonserver.biz.server.dto.GroupUpdateParams;
import com.codeages.javaskeletonserver.biz.server.service.GroupService;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api-admin/group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/search")
    public PagerResponse<GroupDto> search(@RequestParam GroupSearchParams searchParams,
                                          @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                groupService.search(searchParams, pager),
                pager
        );
    }

    @PostMapping("/create")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse create(@RequestBody GroupCreateParams createParams) {
        groupService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse update(@RequestBody GroupUpdateParams updateParams) {
        groupService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        groupService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


