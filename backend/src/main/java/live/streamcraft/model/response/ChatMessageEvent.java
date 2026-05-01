package live.streamcraft.model.response;


public record ChatMessageEvent (String streamId, ChatMessageDto message) {

}
