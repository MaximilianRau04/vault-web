package meety.controllers;

import jakarta.validation.Valid;
import meety.dtos.PollRequestDto;
import meety.dtos.PollResponseDto;
import meety.exceptions.notfound.GroupNotFoundException;
import meety.models.Group;
import meety.models.Poll;
import meety.models.User;
import meety.services.GroupService;
import meety.services.PollService;
import meety.services.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing polls within a specific group.
 * All endpoints are prefixed with /groups/{groupId}/polls.
 */
@RestController
@RequestMapping("/groups/{groupId}/polls")
public class PollController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private PollService pollService;

    @Autowired
    private AuthService authService;

    /**
     * Creates a new poll in the specified group.
     *
     * @param groupId the ID of the group where the poll will be created
     * @param pollDto the poll data sent in the request body
     * @return the created poll as a PollResponseDto
     */
    @PostMapping("")
    public ResponseEntity<PollResponseDto> createPoll(
            @PathVariable Long groupId,
            @RequestBody @Valid PollRequestDto pollDto) {
        User currentUser = authService.getCurrentUser();
        Group group = groupService.getGroupById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));
        Poll poll = pollService.createPoll(group, currentUser, pollDto);

        // Convert to response DTO and return
        PollResponseDto responseDto = pollService.toResponseDto(poll);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Retrieves all polls of a given group.
     *
     * @param groupId the ID of the group
     * @return list of PollResponseDto objects
     */
    @GetMapping("")
    public ResponseEntity<List<PollResponseDto>> getPolls(@PathVariable Long groupId) {
        User currentUser = authService.getCurrentUser();
        List<PollResponseDto> polls = pollService.getPollsByGroup(groupId, currentUser)
                .stream()
                .map(pollService::toResponseDto)
                .toList();
        return ResponseEntity.ok(polls);
    }

    /**
     * Casts a vote for a specific poll option.
     *
     * @param groupId  the ID of the group
     * @param pollId   the ID of the poll
     * @param optionId the ID of the option being voted for
     * @return HTTP 204 No Content
     */
    @PostMapping("/{pollId}/options/{optionId}/vote")
    public ResponseEntity<Void> vote(
            @PathVariable Long groupId,
            @PathVariable Long pollId,
            @PathVariable Long optionId) {
        User currentUser = authService.getCurrentUser();
        pollService.vote(groupId, pollId, optionId, currentUser);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Updates an existing poll.
     *
     * @param groupId the ID of the group
     * @param pollId  the ID of the poll to update
     * @param pollDto the new poll data
     * @return updated PollResponseDto
     */
    @PutMapping("/{pollId}")
    public ResponseEntity<PollResponseDto> updatePoll(
            @PathVariable Long groupId,
            @PathVariable Long pollId,
            @RequestBody @Valid PollRequestDto pollDto) {
        User currentUser = authService.getCurrentUser();
        Poll updatedPoll = pollService.updatePoll(groupId, pollId, currentUser, pollDto);
        return ResponseEntity.ok(pollService.toResponseDto(updatedPoll));
    }

    /**
     * Deletes a poll from a group.
     *
     * @param groupId the ID of the group
     * @param pollId  the ID of the poll to delete
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{pollId}")
    public ResponseEntity<Void> deletePoll(
            @PathVariable Long groupId,
            @PathVariable Long pollId) {
        User currentUser = authService.getCurrentUser();
        pollService.deletePoll(groupId, pollId, currentUser);
        return ResponseEntity.noContent().build();
    }
}