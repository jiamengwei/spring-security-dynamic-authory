package com.example.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@SpringBootTest
class SecurityApplicationTests {
	@Test
	void contextLoads() {
	}

	@Test
	void UsernameTokenDeserializerTest() {
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("admin"));
		authorities.add(new SimpleGrantedAuthority("dev"));

		User user = new User("Jack", "123456", true, true, true, true, authorities);
		UsernamePasswordAuthenticationToken token =
			new UsernamePasswordAuthenticationToken(user, "password", authorities);

		ObjectMapper mapper = new ObjectMapper();
		String tokenStr = null;
		try {
			tokenStr = mapper.writer().writeValueAsString(token);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		SimpleModule module = new SimpleModule();
		module.addDeserializer(UsernamePasswordAuthenticationToken.class, new UsernamePasswordAuthenticationTokenDeserialize());
		mapper.registerModule(module);
		try {
			UsernamePasswordAuthenticationToken token1 = mapper.readValue(tokenStr, UsernamePasswordAuthenticationToken.class);
			User user1 = (User) token1.getPrincipal();
			user1.getAuthorities().forEach(System.out::println);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}


class UsernamePasswordAuthenticationTokenDeserialize extends StdDeserializer<UsernamePasswordAuthenticationToken> {

	public UsernamePasswordAuthenticationTokenDeserialize() {
		this(null);
	}

	public UsernamePasswordAuthenticationTokenDeserialize(Class<UsernamePasswordAuthenticationToken> vc) {
		super(vc);
	}

	@Override
	public UsernamePasswordAuthenticationToken deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		final String username = node.get("principal").findValue("username").asText("unknown user");
		final String password = node.get("principal").findValue("password").asText("unknown password");
		final boolean accountNonExpired = node.get("principal").findValue("accountNonExpired").asBoolean(true);
		final boolean accountNonLocked = node.get("principal").findValue("accountNonLocked").asBoolean(true);
		final boolean credentialsNonExpired = node.get("principal").findValue("credentialsNonExpired").asBoolean(true);
		final boolean enabled = node.get("principal").findValue("enabled").asBoolean();

		final boolean sessionId = node.get("details").findValue("sessionId").asBoolean(true);
		final boolean remoteAddress = node.get("details").findValue("remoteAddress").asBoolean(true);

//		final boolean authenticated = node.get("authenticated").asBoolean();
		final String credentials = node.get("credentials").asText();

		JsonNode authoritiesNode = node.get("authorities");
		Collection<GrantedAuthority> authorities = authoritiesNode.findValues("authority")
			.stream()
			.map(authority -> {
				return new SimpleGrantedAuthority(authority.asText());
			}).collect(Collectors.toList());

		User user = new User(username, password, accountNonExpired, accountNonLocked, credentialsNonExpired, enabled, authorities);
		return new UsernamePasswordAuthenticationToken(user, credentials, authorities);
	}
}