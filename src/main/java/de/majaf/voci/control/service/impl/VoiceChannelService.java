package de.majaf.voci.control.service.impl;

import de.majaf.voci.control.exceptions.InvalidNameException;
import de.majaf.voci.control.exceptions.channel.ChannelDoesNotExistException;
import de.majaf.voci.control.exceptions.channel.InvalidChannelException;
import de.majaf.voci.control.exceptions.user.InvalidUserException;
import de.majaf.voci.control.exceptions.user.UserDoesNotExistException;
import de.majaf.voci.control.service.IChannelService;
import de.majaf.voci.control.service.IRoomService;
import de.majaf.voci.control.service.IUserService;
import de.majaf.voci.control.service.utils.ServiceUtils;
import de.majaf.voci.entity.*;
import de.majaf.voci.entity.repo.VoiceChannelRepository;
import de.mschoettle.entity.dto.FileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Iterator;
import java.util.logging.Logger;

@Service @Scope(value = "singleton")
@Component("voiceChannelService")
public class VoiceChannelService implements IChannelService {

    @Autowired
    private VoiceChannelRepository voiceChannelRepo;

    @Autowired
    private IRoomService roomService;

    @Autowired
    private IUserService userService;

    @Autowired
    private Logger logger;

    @Autowired
    private ServiceUtils serviceUtils;

    @Override
    @Transactional
    public Channel saveChannel(Channel channel) {
        return voiceChannelRepo.save((VoiceChannel) channel);
    }

    @Override
    @Transactional
    public VoiceChannel loadChannelByID(long channelID) throws ChannelDoesNotExistException {
        return voiceChannelRepo.findById(channelID).orElseThrow(() -> new ChannelDoesNotExistException(channelID, "Voice-Channel does not exist"));
    }

    @Override
    @Transactional
    public VoiceChannel loadChannelByIDInRoom(long channelID, Room room) throws ChannelDoesNotExistException, InvalidChannelException {
        VoiceChannel channel = loadChannelByID(channelID);
        if (room.getVoiceChannels().contains(channel)) {
            return channel;
        } else throw new InvalidChannelException(channel, "Channel is not in room.");
    }

    @Override
    @Transactional
    public void addChannelToRoom(Room room, String channelName, RegisteredUser initiator) throws InvalidUserException, InvalidNameException {
        if (initiator.equals(room.getOwner())) {
            if (serviceUtils.checkName(channelName)) {
                VoiceChannel voiceChannel = new VoiceChannel(channelName.trim());
                room.addVoiceChannel(voiceChannel);
                roomService.saveRoom(room);
                logger.info(initiator.getUserName() + " added a new Voice-Channel," + voiceChannel.getChannelName() + ", to room: " + room.getRoomName());
            } else throw new InvalidNameException(channelName, "Channel-Name is not valid");
        } else throw new InvalidUserException(initiator, "User is not Owner");
    }

    @Override
    @Transactional
    public void deleteChannelFromRoom(Room room, long channelID, RegisteredUser initiator) throws InvalidUserException, ChannelDoesNotExistException {
        if (initiator.equals(room.getOwner())) {
            if (room.getVoiceChannels().size() > 1) {
                VoiceChannel voiceChannel = loadChannelByID(channelID);
                room.removeVoiceChannel(voiceChannel);

                for(User member : voiceChannel.getActiveMembers())
                    userService.leaveVoiceChannel(member);

                roomService.saveRoom(room);
                logger.info(initiator.getUserName() + " deleted Voice-Channel," + voiceChannel.getChannelName() + ", from room: " + room.getRoomName());
            }
        } else throw new InvalidUserException(initiator, "User is not Owner");
    }

    @Override
    @Transactional
    public void renameChannel(long channelID, Room room, String channelName, RegisteredUser initiator) throws InvalidNameException, InvalidUserException, ChannelDoesNotExistException, InvalidChannelException {
        if (initiator.equals(room.getOwner())) {
            VoiceChannel channel = loadChannelByID(channelID);
            if (room.getVoiceChannels().contains(channel)) {
                if (serviceUtils.checkName(channelName)) {
                    String oldName = channel.getChannelName();
                    channel.setChannelName(channelName.trim());
                    voiceChannelRepo.save(channel);
                    logger.info(initiator.getUserName() + " changed Voice-Channel-name from" + oldName +"to" + channel.getChannelName() + "in room: " + room.getRoomName());
                } else throw new InvalidNameException(channelName, "Channel-Name is not valid");
            } else throw new InvalidChannelException(channel, "Channel is not in room.");
        } else throw new InvalidUserException(initiator, "User is not Owner");
    }

    @Override
    public boolean userIsInChannel(long channelID, User user) {
        if(user.getActiveVoiceChannel() != null) {
            return user.getActiveVoiceChannel().getId() == channelID;
        }
        return false;
    }
}
