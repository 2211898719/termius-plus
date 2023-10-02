package com.codeages.javaskeletonserver.api.admin;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.javaskeletonserver.biz.server.dto.*;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.util.List;

@RestController
@RequestMapping("/api-admin/server")
public class ServerController {

    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }


    @GetMapping("/list")
    public List<Tree<Long>> findAll() {
        return serverService.findAll();
    }

    @PostMapping("/create")
    public OkResponse create(@RequestBody ServerCreateParams serverCreateParams) {
        serverService.create(serverCreateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody ServerUpdateParams serverUpdateParams) {
        serverService.update(serverUpdateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        serverService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

    @PostMapping("/updateSort")
    public OkResponse updateSort(@RequestBody List<TreeSortParams> treeSortParams) {
        serverService.sort(treeSortParams);

        return OkResponse.TRUE;
    }

}


