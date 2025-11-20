package vaultWeb.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vaultWeb.dtos.GroupDto;
import vaultWeb.models.Group;
import vaultWeb.models.User;
import vaultWeb.security.annotations.AdminOnly;
import vaultWeb.services.GroupService;
import vaultWeb.services.auth.AuthService;

/**
 * Controller for managing groups within the application.
 *
 * <p>Provides endpoints to list public groups, get details of a group, manage group membership, and
 * create, update, or delete groups. Some operations require the user to have admin privileges.
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Group Controller", description = "Manages the different groups a user can join")
public class GroupController {

  private final GroupService groupService;
  private final AuthService authService;

  /**
   * Retrieves all public groups.
   *
   * @return a list of public groups
   */
  @GetMapping("")
  public ResponseEntity<List<Group>> getGroups() {
    List<Group> publicGroups = groupService.getPublicGroups();
    return ResponseEntity.ok(publicGroups);
  }

  /**
   * Retrieves a group by its ID.
   *
   * @param id the ID of the group
   * @return the group if found, or 404 if not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<Group> getGroupById(@PathVariable Long id) {
    return groupService
        .getGroupById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves all members of a given group.
   *
   * @param id the ID of the group
   * @return a list of users in the group
   */
  @GetMapping("/{id}/members")
  public ResponseEntity<List<User>> getGroupMembers(@PathVariable Long id) {
    List<User> members = groupService.getMembers(id);
    return ResponseEntity.ok(members);
  }

  /**
   * Creates a new group.
   *
   * @param groupDto the data for the new group
   * @return the created group
   */
  @PostMapping("")
  public ResponseEntity<Group> createGroup(@RequestBody GroupDto groupDto) {
    User currentUser = authService.getCurrentUser();
    Group created = groupService.createGroup(groupDto, currentUser);
    return ResponseEntity.ok(created);
  }

  /**
   * Current user joins a group.
   *
   * @param id the ID of the group to join
   * @return the updated group
   */
  @PostMapping("/{id}/join")
  public ResponseEntity<Group> joinGroup(@PathVariable Long id) {
    User currentUser = authService.getCurrentUser();
    Group updatedGroup = groupService.joinGroup(id, currentUser);
    return ResponseEntity.ok(updatedGroup);
  }

  /**
   * Updates a group. Admin privileges required.
   *
   * @param id the ID of the group to update
   * @param updatedGroup the updated group data
   * @return the updated group
   */
  @AdminOnly
  @PutMapping("/{id}")
  public ResponseEntity<Group> updateGroup(
      @PathVariable Long id, @RequestBody GroupDto updatedGroup) {
    return ResponseEntity.ok(groupService.updateGroup(id, updatedGroup));
  }

  /**
   * Deletes a group. Admin privileges required.
   *
   * @param id the ID of the group to delete
   */
  @AdminOnly
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
    groupService.deleteGroup(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Current user leaves a group.
   *
   * @param id the ID of the group to leave
   * @return the updated group
   */
  @DeleteMapping("/{id}/leave")
  public ResponseEntity<Group> leaveGroup(@PathVariable Long id) {
    User currentUser = authService.getCurrentUser();
    Group updatedGroup = groupService.leaveGroup(id, currentUser);
    return ResponseEntity.ok(updatedGroup);
  }

  /**
   * Removes a member from a group. Admin privileges required.
   *
   * @param groupId the ID of the group
   * @param userId the ID of the user to remove
   * @return the updated group
   */
  @AdminOnly
  @DeleteMapping("/{groupId}/members/{userId}")
  public ResponseEntity<Group> removeMemberFromGroup(
      @PathVariable Long groupId, @PathVariable Long userId) {
    Group group = groupService.removeMember(groupId, userId);
    return ResponseEntity.ok(group);
  }
}
