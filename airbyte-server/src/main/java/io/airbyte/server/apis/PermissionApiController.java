/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis;

import static io.airbyte.commons.auth.AuthRoleConstants.ADMIN;
import static io.airbyte.commons.auth.AuthRoleConstants.ORGANIZATION_ADMIN;
import static io.airbyte.commons.auth.AuthRoleConstants.ORGANIZATION_READER;
import static io.airbyte.commons.auth.AuthRoleConstants.SELF;
import static io.airbyte.commons.auth.AuthRoleConstants.WORKSPACE_ADMIN;
import static io.airbyte.commons.auth.AuthRoleConstants.WORKSPACE_READER;

import io.airbyte.api.generated.PermissionApi;
import io.airbyte.api.model.generated.PermissionCheckRead;
import io.airbyte.api.model.generated.PermissionCheckRequest;
import io.airbyte.api.model.generated.PermissionCreate;
import io.airbyte.api.model.generated.PermissionDeleteUserFromWorkspaceRequestBody;
import io.airbyte.api.model.generated.PermissionIdRequestBody;
import io.airbyte.api.model.generated.PermissionRead;
import io.airbyte.api.model.generated.PermissionReadList;
import io.airbyte.api.model.generated.PermissionType;
import io.airbyte.api.model.generated.PermissionUpdate;
import io.airbyte.api.model.generated.PermissionsCheckMultipleWorkspacesRequest;
import io.airbyte.api.model.generated.UserIdRequestBody;
import io.airbyte.commons.annotation.AuditLogging;
import io.airbyte.commons.annotation.AuditLoggingProvider;
import io.airbyte.commons.server.handlers.PermissionHandler;
import io.airbyte.commons.server.scheduling.AirbyteTaskExecutors;
import io.airbyte.validation.json.JsonValidationException;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

/**
 * This class is migrated from cloud-server PermissionApiController
 * {@link io.airbyte.cloud.server.apis.PermissionApiController}.
 *
 * TODO: migrate all Permission endpoints (including some endpoints in WebBackend API) from Cloud to
 * OSS.
 */
@Controller("/api/v1/permissions")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(AirbyteTaskExecutors.IO)
public class PermissionApiController implements PermissionApi {

  private final PermissionHandler permissionHandler;

  public PermissionApiController(final PermissionHandler permissionHandler) {
    this.permissionHandler = permissionHandler;
  }

  @Secured({ORGANIZATION_ADMIN, WORKSPACE_ADMIN})
  @Post("/create")
  @Override
  @AuditLogging(provider = AuditLoggingProvider.CREATE_PERMISSION)
  public PermissionRead createPermission(@Body final PermissionCreate permissionCreate) {
    return ApiHelper.execute(() -> {
      validatePermissionCreation(permissionCreate);
      return permissionHandler.createPermission(permissionCreate);
    });
  }

  private void validatePermissionCreation(final PermissionCreate permissionCreate) throws JsonValidationException {
    if (permissionCreate.getPermissionType() == PermissionType.INSTANCE_ADMIN) {
      throw new JsonValidationException("Instance Admin permissions cannot be created via API.");
    }
    if (permissionCreate.getOrganizationId() == null && permissionCreate.getWorkspaceId() == null) {
      throw new JsonValidationException("Either workspaceId or organizationId should be provided.");
    }
  }

  @Secured({ORGANIZATION_READER, WORKSPACE_READER})
  @Post("/get")
  @Override
  public PermissionRead getPermission(@Body final PermissionIdRequestBody permissionIdRequestBody) {
    return ApiHelper.execute(() -> permissionHandler.getPermission(permissionIdRequestBody));
  }

  @Secured({ORGANIZATION_ADMIN, WORKSPACE_ADMIN})
  @Post("/update")
  @Override
  @AuditLogging(provider = AuditLoggingProvider.UPDATE_PERMISSION)
  public void updatePermission(@Body final PermissionUpdate permissionUpdate) {
    ApiHelper.execute(() -> {
      validatePermissionUpdate(permissionUpdate);
      permissionHandler.updatePermission(permissionUpdate);
      return null;
    });
  }

  private void validatePermissionUpdate(@Body final PermissionUpdate permissionUpdate) throws JsonValidationException {
    if (permissionUpdate.getPermissionType() == PermissionType.INSTANCE_ADMIN) {
      throw new JsonValidationException("Cannot modify Instance Admin permissions via API.");
    }
  }

  @Secured({ORGANIZATION_ADMIN, WORKSPACE_ADMIN})
  @Post("/delete")
  @Override
  @AuditLogging(provider = AuditLoggingProvider.DELETE_PERMISSION)
  public void deletePermission(@Body final PermissionIdRequestBody permissionIdRequestBody) {

    ApiHelper.execute(() -> {
      permissionHandler.deletePermission(permissionIdRequestBody);
      return null;
    });
  }

  @Secured({ORGANIZATION_ADMIN, WORKSPACE_ADMIN})
  @Post("/delete_user_from_workspace")
  @Override
  public void deleteUserFromWorkspace(@Body final PermissionDeleteUserFromWorkspaceRequestBody permissionDeleteUserFromWorkspaceRequestBody) {
    ApiHelper.execute(() -> {
      permissionHandler.deleteUserFromWorkspace(permissionDeleteUserFromWorkspaceRequestBody);
      return null;
    });
  }

  @Secured({ADMIN, SELF})
  @Post("/list_by_user")
  @Override
  public PermissionReadList listPermissionsByUser(@Body final UserIdRequestBody userIdRequestBody) {
    return ApiHelper.execute(() -> permissionHandler.listPermissionsByUser(userIdRequestBody.getUserId()));
  }

  @Secured({ADMIN}) // instance admins only
  @Post("/check")
  @Override
  public PermissionCheckRead checkPermissions(@Body final PermissionCheckRequest permissionCheckRequest) {

    return ApiHelper.execute(() -> permissionHandler.checkPermissions(permissionCheckRequest));
  }

  @Secured({ADMIN}) // instance admins only
  @Post("/check_multiple_workspaces")
  @Override
  public PermissionCheckRead checkPermissionsAcrossMultipleWorkspaces(@Body final PermissionsCheckMultipleWorkspacesRequest request) {
    return ApiHelper.execute(() -> permissionHandler.permissionsCheckMultipleWorkspaces(request));
  }

}
