package live.streamcraft.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import live.streamcraft.exception.TooManyRequestsException;

@Service
public class RateLimiterService {
	private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> timestamps = new ConcurrentHashMap<>();

    private static final int LIMIT = 5;
    private static final long WINDOW = 60_000;
    
    public void validate(String key) {
    	long now = System.currentTimeMillis();
    	
    	timestamps.putIfAbsent(key, now);
    	attempts.putIfAbsent(key, 0);
    	
    	if(now - timestamps.get(key) > WINDOW) {
    		timestamps.put(key, now);
    		attempts.put(key, 0);
    	}
    	
    	int count = attempts.get(key);
    	
    	if(count >= LIMIT) {
    		throw new TooManyRequestsException("Too many requests");
    	}
    	
    	attempts.put(key, count + 1);
    }
}