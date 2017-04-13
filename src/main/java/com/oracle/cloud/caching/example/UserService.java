package com.oracle.cloud.caching.example;

import java.util.Optional;

import com.oracle.cloud.cache.basic.Cache;
import com.oracle.cloud.cache.basic.RemoteSessionProvider;
import com.oracle.cloud.cache.basic.Session;
import com.oracle.cloud.cache.basic.SessionProvider;
import com.oracle.cloud.cache.basic.options.Return;
import com.oracle.cloud.cache.basic.options.Transport;

public class UserService {
	
	private static final String CACHE_HOST = System.getenv("CACHING_INTERNAL_CACHE_URL");
	private static final Optional<String> CACHE_PROTOCOL = Optional.ofNullable(System.getenv("CACHING_PROTOCOL"));

	private Session cacheSession;
	private Cache<User> users;


	public UserService() {
		super();
		initCache();
	}

	public User getUser(String id) {
		return users.get(id);
	}

	public User createUser(String name, String email) {
		failIfInvalid(name, email);
		User user = new User(name, email);
		users.put(user.getId(),user);
		return user;
	}

	public User updateUser(String id, String name, String email) {
		User user = getUser(id);
		if (user == null) {
			throw new IllegalArgumentException("No user with id '" + id + "' found");
		}
		failIfInvalid(name, email);
		user.setName(name);
		user.setEmail(email);
		users.replace(id, user);
		return user;
	}

	public User deleteUser(String id) {
		User user = users.remove(id, Return.OLD_VALUE);
		if (null == user) {
			throw new IllegalArgumentException("No user with id '" + id + "' found");
		} 
		return user;
	}

	private void failIfInvalid(String name, String email) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Parameter 'name' cannot be empty");
		}
		if (email == null || email.isEmpty()) {
			throw new IllegalArgumentException("Parameter 'email' cannot be empty");
		}
	}
	
	protected void initCache() {
		String protocolName = CACHE_PROTOCOL.orElse("REST").toUpperCase();
		String port = null;
		String cacheUrlSuffix = "";
		Transport transport = null;
		switch (protocolName) {
		case "GRPC":
			port = "1444";
			transport = Transport.grpc();
			break;
		default: // REST
			port = "8080";
			transport = Transport.rest();
			cacheUrlSuffix = "ccs";
			break;
		}	
		String cacheUrl = "http://" + CACHE_HOST + ":" + port + "/" + cacheUrlSuffix;
		SessionProvider sessionProvider = new RemoteSessionProvider(cacheUrl);
		cacheSession = sessionProvider.createSession(transport);
		System.err.println("[debug] cacheSession=" + cacheSession);
		if (null != cacheSession) {
			users = cacheSession.getCache("users");
			System.err.println("[debug] cache=" + users);
		} else {
			System.err.println("[debug] Cannot get cache.");
		}
	}
}
