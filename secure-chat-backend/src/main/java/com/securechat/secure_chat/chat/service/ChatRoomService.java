package com.securechat.secure_chat.chat.service;

import com.securechat.secure_chat.chat.dto.CreateRoomRequest;
import com.securechat.secure_chat.domain.chat.ChatRoom;
import com.securechat.secure_chat.domain.chat.ChatRoomRepository;
import com.securechat.secure_chat.domain.user.User;
import com.securechat.secure_chat.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoom createRoom(String username, CreateRoomRequest request) {
        User user = findUser(username);

        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .type(request.getType())
                .createdBy(user)
                .inviteCode(ChatRoom.generateInviteCode())
                .build();
        room.addMember(user);

        ChatRoom saved = chatRoomRepository.save(room);
        log.info("채팅방 생성 - roomId: {}, name: {}, inviteCode: {}", saved.getId(), saved.getName(), saved.getInviteCode());
        return saved;
    }
    // 초대 링크로 채팅방 입장
    @Transactional
    public ChatRoom joinByInviteCode(String inviteCode, String username){
        ChatRoom room = chatRoomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "유효하지 않은 초대 코드입니다: " + inviteCode));
        User user = findUser(username);
        if (room.hasMember(user.getId())) {
            return room;
        }

        room.addMember(user);
        log.info("초대 코드로 채팅방 입장 - roomId: {}, username: {}", room.getId(), username);
        return room;
    }

    //초대 링크 생성
    @Transactional
    public String generateInviteLink(UUID roomId, String username){
        ChatRoom room = findRoom(roomId);

        if(!room.hasMember(findUser(username).getId())){
            throw new SecurityException("채팅방 참여자가 아닙니다");
        }
        return "http://localhost:3000/join/" + room.getInviteCode();
    }

    @Transactional
    public void joinRoom(UUID roomId, String username) {
        ChatRoom room = findRoom(roomId);
        User user = findUser(username);

        if (room.getType() == com.securechat.secure_chat.domain.chat.ChatRoomType.SECRET) {
            throw new IllegalStateException("시크릿 채팅방은 초대로만 입장 가능합니다");
        }
        if (room.hasMember(user.getId())) {
            return;
        }

        room.addMember(user);
        log.info("채팅방 입장 - roomId: {}, username: {}", roomId, username);
    }

    @Transactional
    public void leaveRoom(UUID roomId, String username){
        ChatRoom room = findRoom(roomId);
        User user = findUser(username);

        if(!room.hasMember(user.getId())){
            throw new IllegalArgumentException("채팅방 참여자가 아닙니다");
        }

        room.removeMember(user);
        log.info("채팅방 나가기 - roomId: {}, username: {}", roomId, username);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getMyRooms(String username) {
        User user = findUser(username);
        return chatRoomRepository.findByMembersId(user.getId());
    }

    @Transactional(readOnly = true)
    public ChatRoom validateMembership(UUID roomId, String username) {
        ChatRoom room = findRoom(roomId);
        User user = findUser(username);

        if (!room.hasMember(user.getId())) {
            throw new SecurityException("채팅방 참여자가 아닙니다");
        }
        return room;
    }
    

    private ChatRoom findRoom(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "채팅방을 찾을 수 없습니다: " + roomId));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다: " + username));
    }
}