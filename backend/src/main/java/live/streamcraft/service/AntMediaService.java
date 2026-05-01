package live.streamcraft.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import live.streamcraft.model.response.AntMediaBroadcast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AntMediaService {

    private final RestTemplate restTemplate;
    
    @Value("${ant.media.url:http://localhost:5080}")
    private String antMediaUrl;
    
    @Value("${ant.media.app.name:WebRTCApp}")
    private String appName;
    
    @Value("${ant.media.stream.ingest-endpoint:rtmp://localhost:1935/LiveApp}")
    private String rtmpIngestBase;
    
    @Value("${ant.media.stream.hls-prefix:http://localhost:5080/LiveApp/streams}")
    private String hlsPrefix;
    
    @Value("${ant.media.stream.webrtc-prefix:https://localhost:5443/LiveApp/webrtc}")
    private String webRtcPrefix;
    
    private String apiBaseUrl;
    private String broadcastsEndpoint;
    
    @PostConstruct
    public void init() {
        // REST API endpoint: http://localhost:5080/WebRTCApp/rest/v2/
        apiBaseUrl = String.format("%s/%s/rest/v2", antMediaUrl, appName);
        broadcastsEndpoint = apiBaseUrl + "/broadcasts";
        log.info("AntMediaService initialized with API base URL: {}", apiBaseUrl);
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    public AntMediaBroadcast createBroadcast(String streamId, String streamName, String description) {
        String url = broadcastsEndpoint + "/create";
        
        Map<String, Object> requestBody = Map.of(
            "streamId", streamId,
            "name", streamName != null ? streamName : streamId,
            "description", description != null ? description : "",
            "type", "liveStream"
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createHeaders());
        
        try {
            log.info("Creating broadcast: {} at {}", streamId, url);
            ResponseEntity<AntMediaBroadcast> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, AntMediaBroadcast.class
            );
            
            AntMediaBroadcast broadcast = response.getBody();
            if (broadcast != null) {
                log.info("Broadcast created: {} with RTMP: {}", broadcast.getStreamId(), broadcast.getRtmpURL());
            }
            return broadcast;
        } catch (RestClientException e) {
            log.error("Failed to create broadcast: {}", e.getMessage());
            throw new RuntimeException("Could not create broadcast: " + e.getMessage(), e);
        }
    }
    
    public boolean startBroadcast(String streamId) {
        String url = broadcastsEndpoint + "/" + streamId + "/start";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Failed to start broadcast {}: {}", streamId, e.getMessage());
            return false;
        }
    }
    
    public boolean stopBroadcast(String streamId) {
        String url = broadcastsEndpoint + "/" + streamId + "/stop";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Failed to stop broadcast {}: {}", streamId, e.getMessage());
            return false;
        }
    }
    
    public boolean deleteBroadcast(String streamId) {
        String url = broadcastsEndpoint + "/" + streamId;
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Failed to delete broadcast {}: {}", streamId, e.getMessage());
            return false;
        }
    }
    
    public Optional<AntMediaBroadcast> getBroadcast(String streamId) {
        String url = broadcastsEndpoint + "/" + streamId;
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        
        try {
            ResponseEntity<AntMediaBroadcast> response = restTemplate.exchange(url, HttpMethod.GET, entity, AntMediaBroadcast.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.warn("Failed to get broadcast {}: {}", streamId, e.getMessage());
            return Optional.empty();
        }
    }
    
    public String getRtmpIngestUrl(String streamId) {
        return String.format("%s/%s", rtmpIngestBase, streamId);
    }
    
    public String getHlsPlaybackUrl(String streamId) {
        return String.format("%s/%s.m3u8", hlsPrefix, streamId);
    }
    
    public String getWebRtcPlaybackUrl(String streamId) {
        return String.format("%s?streamId=%s", webRtcPrefix, streamId);
    }
    
    public String getPlayerUrl(String streamId) {
        return String.format("http://localhost:5080/%s/play.html?name=%s", appName, streamId);
    }
}