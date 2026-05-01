package live.streamcraft.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.Chat;
import live.streamcraft.entity.Stream;
import live.streamcraft.entity.User;
import live.streamcraft.exception.ForbiddenException;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.exception.UnauthorizedException;
import live.streamcraft.model.request.ChatMessageRequest;
import live.streamcraft.model.response.ChatMessageDto;
import live.streamcraft.model.response.ChatMessageEvent;
import live.streamcraft.repository.ChatRepository;
import live.streamcraft.repository.StreamRepository;
import live.streamcraft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
	private final ChatRepository chatRepository;
    private final StreamRepository streamRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send a chat message to a stream
     */
    public ChatMessageDto sendMessage(String userId, String streamId, ChatMessageRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        
        Stream stream = streamRepository.findByIdWithDetails(streamId)
            .orElseThrow(() -> new NotFoundException("Stream not found: " + streamId));
        
        if (!stream.isLive()) {
            throw new IllegalStateException("Cannot send messages to a stream that is not live");
        }
        
        // Check if user can chat (not banned/muted)
        if (!stream.canUserChat(userId)) {
            throw new ForbiddenException("You are banned or muted from this stream");
        }
        
        // Check if chat is enabled
        if (!stream.isChatEnabled()) {
            throw new IllegalStateException("Chat is disabled for this stream");
        }
        
        // Create and save chat message
        Chat chat = Chat.builder()
            .message(request.getMessage())
            .user(user)
            .stream(stream)
            .build();
        
        Chat savedChat = chatRepository.save(chat);
        
        ChatMessageDto response = toChatMessageResponse(savedChat);
          
        // Send real-time message via WebSocket
        ChatMessageEvent event = new ChatMessageEvent(streamId, response);
        
        messagingTemplate.convertAndSend("/topic/streams/" + streamId + "/chat", event);
        
        log.info("Message sent to stream {} by user {}", streamId, userId);
        
        return response;
    }
    
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getChatMessages(String streamId, Pageable pageable) {
        // Verify stream exists
        if (!streamRepository.existsById(streamId)) {
            throw new NotFoundException("Stream not found: " + streamId);
        }
        
        Page<Chat> chatPage = chatRepository.findByStreamIdOrderByCreatedAtAsc(streamId, pageable);
        
        return chatPage.map(this::toChatMessageResponse);
    }
    
    @Transactional
    public List<ChatMessageDto> getRecentChatMessages(String streamId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        Page<Chat> chatPage = chatRepository.findByStreamIdOrderByCreatedAtDesc(streamId, pageable);
        
        return chatPage.getContent().stream()
            .map(this::toChatMessageResponse)
            .collect(Collectors.toList());
    }
    
    public void deleteMessage(String userId, String messageId, String streamId) {
        Chat chat = chatRepository.findById(messageId)
            .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));
        
        Stream stream = streamRepository.findById(streamId)
            .orElseThrow(() -> new NotFoundException("Stream not found: " + streamId));
        
        // Check permission: streamer or admin
        boolean isStreamer = stream.getStreamer().getId().equals(userId);
        boolean isAdmin = isAdmin(userId);
        
        if (!isStreamer && !isAdmin) {
            throw new UnauthorizedException("Not authorized to delete this message");
        }
        
        chat.setDeleted(true);
        chat.setDeletedAt(LocalDateTime.now());
        chatRepository.save(chat);
        
        log.info("Message {} deleted by user {}", messageId, userId);
    }
    
    public void clearChatHistory(String userId, String streamId) {
        Stream stream = streamRepository.findByIdWithDetails(streamId)
            .orElseThrow(() -> new NotFoundException("Stream not found: " + streamId));
        
        if (!stream.getStreamer().getId().equals(userId)) {
            throw new UnauthorizedException("Only the streamer can clear chat history");
        }
        
        chatRepository.softDeleteByStreamId(streamId, LocalDateTime.now());
        log.info("Chat history cleared for stream {} by user {}", streamId, userId);
    }
    
    // Helper method
    private ChatMessageDto toChatMessageResponse(Chat chat) {
    	return new ChatMessageDto(chat.getId(), chat.getStream().getId(), chat.getUser().getId(), chat.getMessage(), 
    			chat.getUser().getUname(), chat.getUser().getAvatarUrl(), chat.getCreatedAt());

    }
    
    private boolean isAdmin(String userId) {
        return userRepository.findById(userId)
            .map(user -> user.getRole() != null && "ADMIN".equals(user.getRole().getName()))
            .orElse(false);
    }
}
