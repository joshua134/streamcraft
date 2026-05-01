package live.streamcraft.v1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import live.streamcraft.model.request.AntMediaWebhookPayload;
import live.streamcraft.service.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebHookController {
	private final StreamService streamService;
    
    @Value("${streaming.webhook.secret:}")
    private String webhookSecret;
    
    //api/webhooks/ant-media
    @PostMapping("/ant-media")
    public ResponseEntity<Void> handleAntMediaWebhook(@RequestBody AntMediaWebhookPayload payload,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
        
        if (webhookSecret != null && !webhookSecret.isEmpty() && !webhookSecret.equals(secret)) {
            log.warn("Invalid webhook secret received");
            return ResponseEntity.status(401).build();
        }
        
        log.debug("Webhook received: action={}, streamId={}", payload.getAction(), payload.getStreamId());
        streamService.handleWebhook(payload);
        
        return ResponseEntity.ok().build();
    }
}
