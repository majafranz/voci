package de.majaf.voci.control.service.impl;

import de.majaf.voci.control.exceptions.call.InvitationTokenDoesNotExistException;
import de.majaf.voci.control.service.ICallService;
import de.majaf.voci.control.service.IChannelService;
import de.majaf.voci.control.service.IUserService;
import de.majaf.voci.control.exceptions.call.InvalidCallStateException;
import de.majaf.voci.control.exceptions.call.InvitationDoesNotExistException;
import de.majaf.voci.control.exceptions.user.InvalidUserException;
import de.majaf.voci.control.exceptions.user.UserDoesNotExistException;
import de.majaf.voci.entity.*;
import de.majaf.voci.entity.repo.CallRepository;
import de.majaf.voci.entity.repo.InvitationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Service
@Scope(value = "singleton")
public class CallService extends ExternalCallService implements ICallService {

    @Autowired
    private InvitationRepository invitationRepo;

    @Autowired
    private CallRepository callRepo;

    @Autowired
    private IUserService userService;

    @Autowired
    private Logger logger;

    @Override
    @Transactional
    public List<Call> getAllCalls() {
        return (List<Call>) callRepo.findAll();
    }

    @Override
    @Transactional
    public Invitation createInvitation(RegisteredUser initiator) {
        Invitation invitation = new Invitation(initiator);
        initiator.setOwnedInvitation(invitation);
        invitationRepo.save(invitation);
        logger.info("Invitation was created for " + initiator.getUserName());
        return invitation;
    }

    @Override
    @Transactional
    public void inviteToCall(Invitation invitation, long invitedContactID) throws InvalidUserException, UserDoesNotExistException {
        RegisteredUser invited = (RegisteredUser) userService.loadUserByID(invitedContactID);
        if (!invitation.getInitiator().getContacts().contains(invited))
            throw new InvalidUserException(invited, "User not in contacts.");

        invited.addActiveInvitation(invitation);
        invitation.addInvitedUser(invited);
        logger.info(invitation.getInitiator().getUserName() + " invited " + invited.getUserName() + " to a call.");
        invitationRepo.save(invitation);
    }

    @Override
    public void uninviteUserByID(Invitation invitation, long invitedContactID) throws UserDoesNotExistException {
        RegisteredUser invited = (RegisteredUser) userService.loadUserByID(invitedContactID);
        uninviteUser(invitation, invited);
    }

    @Transactional
    @Override
    public void uninviteUser(Invitation invitation, RegisteredUser invited) {
        invited.removeActiveInvitation(invitation);
        invitation.removeInvitedUser(invited);
        invitationRepo.save(invitation);
        logger.info(invitation.getInitiator().getUserName() + " uninvited " + invited.getUserName() + " from call.");
    }

    @Transactional
    @Override
    public void joinCallByAccessToken(User user, String accessToken) throws InvalidUserException, InvalidCallStateException, InvitationTokenDoesNotExistException {
        joinCall(user, loadInvitationByToken(accessToken));
    }

    @Transactional
    @Override
    public void joinCall(User user, Invitation invitation) throws InvalidUserException, InvalidCallStateException {
        if (user instanceof RegisteredUser)
            if (!invitation.getInvitedUsers().contains(user) && !user.equals(invitation.getInitiator()))
                throw new InvalidUserException(user, "User is not invited");

        Call call = invitation.getCall();
        if (call == null) throw new InvalidCallStateException(call, "Call is not active");

        Call userCall = user.getActiveCall();
        if (userCall != null) {         // check if user is active in other call
            if (!userCall.equals(call)) { // nothing happens if the user is already in the right call
                if (user instanceof GuestUser)
                    throw new InvalidUserException(user, "Guest can not join an other call.");
                leaveCall(user);
                joinCall(call, user);
            }
        } else {
            joinCall(call, user);
        }
        logger.info(user.getUserName() + " joined call from " + invitation.getInitiator().getUserName());
    }

    private void joinCall(Call call, User user) {
        VoiceChannel voiceChannel = call.getVoiceChannel();
        voiceChannel.addActiveMember(user);
        user.setActiveVoiceChannel(voiceChannel);

        user.setActiveCall(call);
        call.addParticipant(user);
        userService.saveUser(user);
    }

    @Transactional
    @Override
    public Call leaveCall(User user) throws InvalidCallStateException {
        Call call = user.getActiveCall();
        if (call != null) {

            // Call is reloaded, because of lazy-initialization
            Optional<Call> c = callRepo.findById(call.getId());
            if (c.isPresent()) {
                call = c.get();

                call.removeParticipant(user);
                user.setActiveCall(null);

                VoiceChannel voiceChannel = call.getVoiceChannel();
                voiceChannel.removeActiveMember(user);
                user.setActiveVoiceChannel(null);

                if (call.getParticipants().isEmpty()) {
                    endCall(call);
                    logger.info(user.getUserName() + " was the last participant in a call. Call ended");
                }
                else if (user instanceof RegisteredUser) {
                    Invitation invitation = call.getInvitation();
                    if (invitation != null && invitation.equals(((RegisteredUser) user).getOwnedInvitation()))
                        endInvitation(invitation);
                    userService.saveUser(user);
                }
                logger.info(user.getUserName() + " left a call.");
                return call;
            } else return null;
        } else throw new InvalidCallStateException(call, "User has no active Call");
    }

    @Override
    @Transactional
    public List<Long> checkCallsForTimeoutOrEnd(long timediff) {
        List<Call> calls = getAllCalls();

        // this is the workaround for informing the frontend which calls have ended
        List<Long> endedCallIDs = new ArrayList<>();

        for (Call call : calls) {

            if (call.getTimeout() <= 0) {
                endedCallIDs.add(call.getId());
                endCall(call);
            } else {
                call.setTimeout(call.getTimeout() - timediff);
            }

        }
        logger.info("Currently open calls: " + (calls.size() - endedCallIDs.size()));
        logger.info("Timed-out calls: " + endedCallIDs.size());
        return endedCallIDs;
    }
}
