package ua.ardas.esputnik.events.spring;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.Message;

import com.google.gson.Gson;

import ua.ardas.esputnik.events.queue.EventRedisDto;

@Configuration
@EnableIntegration
public class RedisConfig {

	@Bean
	public ExecutorChannel rowChannel() {
		return new ExecutorChannel(Executors.newFixedThreadPool(10));
	}

	@Bean
	public DirectChannel objChannel() {
		return new DirectChannel();
	}

	@Bean
	public DirectChannel errorChannel() {
		return new DirectChannel();
	}

	@Bean
	@Transformer(inputChannel = "rowChannel", outputChannel = "objChannel")
	public AbstractTransformer transformer() {
		return new AbstractTransformer() {
			@Override
			protected Object doTransform(Message<?> message) {
				return new Gson().fromJson((String) message.getPayload(), EventRedisDto.class);
			}
		};
	}

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisClusterConfiguration configuration = new RedisClusterConfiguration();
		configuration.setClusterNodes(Arrays.asList(new RedisNode("localhost", 7000), new RedisNode("localhost", 7001)));
		return new LettuceConnectionFactory(configuration);
	}

	@Bean
	public RedisQueueMessageDrivenEndpoint redisEventsConnector(RedisConnectionFactory connectionFactory) {
		RedisQueueMessageDrivenEndpoint redisQueueMessageDrivenEndpoint =
				new RedisQueueMessageDrivenEndpoint("events:by:organisations", connectionFactory);
		redisQueueMessageDrivenEndpoint.setOutputChannel(rowChannel());
		redisQueueMessageDrivenEndpoint.setSerializer(new StringRedisSerializer());
		redisQueueMessageDrivenEndpoint.setTaskExecutor(Executors.newFixedThreadPool(10));
		redisQueueMessageDrivenEndpoint.setErrorChannel(errorChannel());
		return redisQueueMessageDrivenEndpoint;
	}

	@Bean
	public RedisQueueMessageDrivenEndpoint redisDBEventsConnector(RedisConnectionFactory connectionFactory) {
		RedisQueueMessageDrivenEndpoint redisQueueMessageDrivenEndpoint =
				new RedisQueueMessageDrivenEndpoint("events:dbredis", connectionFactory);
		redisQueueMessageDrivenEndpoint.setOutputChannel(rowChannel());
		redisQueueMessageDrivenEndpoint.setSerializer(new StringRedisSerializer());
		redisQueueMessageDrivenEndpoint.setTaskExecutor(Executors.newFixedThreadPool(10));
		redisQueueMessageDrivenEndpoint.setErrorChannel(errorChannel());
		return redisQueueMessageDrivenEndpoint;
	}

	public DataSource msSqlDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		dataSource.setUrl("jdbc:sqlserver://localhost:1433;database=es_test");
		dataSource.setUsername("sa");
		dataSource.setPassword("secret");
		return dataSource;
	}

	@Bean
	@InboundChannelAdapter(channel = "objChannel", poller = @Poller(fixedDelay = "5000"))
	public JdbcPollingChannelAdapter msEventsConnector() {
		JdbcPollingChannelAdapter jdbcPollingChannelAdapter = new JdbcPollingChannelAdapter(msSqlDataSource(), "Select * from Events");
		jdbcPollingChannelAdapter.setRowMapper(mapper());
		return jdbcPollingChannelAdapter;
	}

	public RowMapper<EventRedisDto> mapper() {
		return (rs, rowNum) -> EventRedisDto.builder()
				.date(new Date())
				.eventTypeId(rs.getInt("EventTypeID"))
				.json(rs.getString("Params"))
				.keyValue(rs.getString("Key"))
				.organisationId(rs.getInt("OrganisationID"))
				.build();
	}

}