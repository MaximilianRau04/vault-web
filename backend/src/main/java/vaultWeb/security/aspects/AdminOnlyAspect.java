package vaultWeb.security.aspects;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import vaultWeb.exceptions.AdminAccessDeniedException;
import vaultWeb.models.GroupMember;
import vaultWeb.models.User;
import vaultWeb.models.enums.Role;
import vaultWeb.repositories.GroupMemberRepository;
import vaultWeb.services.auth.AuthService;

/**
 * Aspect that enforces admin-only access for methods annotated with {@link
 * vaultWeb.security.annotations.AdminOnly}.
 *
 * <p>This aspect intercepts method calls and verifies that the currently authenticated user has an
 * ADMIN role in the specified group. If the user is not authenticated or does not have admin
 * privileges, an {@link AdminAccessDeniedException} is thrown.
 *
 * <p>Methods annotated with {@code @AdminOnly} must have the group ID as the first parameter (of
 * type {@link Long}) to allow the aspect to verify the user's role within that group.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AdminOnlyAspect {

  private final AuthService authService;
  private final GroupMemberRepository groupMemberRepository;

  /**
   * Advice that runs before any method annotated with {@link
   * vaultWeb.security.annotations.AdminOnly}.
   *
   * <p>Checks if the current user is authenticated and has an ADMIN role in the specified group.
   * Throws {@link AdminAccessDeniedException} if the user is not authenticated or not an admin.
   *
   * @param joinPoint the join point providing access to the method being invoked and its arguments
   * @throws AdminAccessDeniedException if the user is not authenticated or does not have admin
   *     privileges
   * @throws IllegalArgumentException if the method does not have a group ID (Long) as its first
   *     argument
   */
  @Before("@annotation(vaultWeb.security.annotations.AdminOnly)")
  public void checkAdmin(JoinPoint joinPoint) {
    User currentUser = authService.getCurrentUser();

    if (currentUser == null) {
      throw new AdminAccessDeniedException("Not authenticated.");
    }

    Object[] args = joinPoint.getArgs();
    if (args.length == 0 || !(args[0] instanceof Long)) {
      throw new IllegalArgumentException(
          "Method with @AdminOnly must have groupId (Long) as first argument.");
    }

    Long groupId = (Long) args[0];

    Optional<GroupMember> groupMemberOpt =
        groupMemberRepository.findByGroupIdAndUserId(groupId, currentUser.getId());

    if (groupMemberOpt.isEmpty() || groupMemberOpt.get().getRole() != Role.ADMIN) {
      throw new AdminAccessDeniedException("Admin privileges for this group required.");
    }
  }
}
