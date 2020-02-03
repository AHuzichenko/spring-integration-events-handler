package ua.ardas.esputnik.events;

import com.jayway.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {EventsApplication.class,})
@ContextConfiguration(initializers = BaseServiceTest.Initializer.class)
@Slf4j
public abstract class BaseServiceTest {

	@ClassRule
	public static CassandraContainer cassandra = new CassandraContainer("cassandra:3.11.1");

	@ClassRule
	public static GenericContainer redis =
			new GenericContainer("esputnik/redis-cluster:latest")
					.withExposedPorts(7000, 7001, 7002, 7003, 7004, 7005, 7006, 7007)
					.waitingFor(new LogMessageWaitStrategy().withRegEx(".*/var/log/supervisor/redis-6.log.*\\s"));

	@Value("${local.server.port:0}")
	private int port;

	@Before
	public void setup() {
		RestAssured.port = port;
	}

	public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			Integer mappedPort = redis.getMappedPort(7000);
			String containerIpAddress = redis.getContainerIpAddress();
			log.info("Redis docker port : {} host : {}", mappedPort, containerIpAddress);
			CSDataSetWatcher.ensureKeyspaceAndSession(cassandra.getCluster());
			System.setProperty("cassandra.port", cassandra.getMappedPort(9042).toString());
			System.setProperty("spring.data.cassandra.port", cassandra.getMappedPort(9042).toString());
			TestPropertyValues
					.of(
							"management.health.cassandra.enabled=false",
							"management.health.redis.enabled=false",
							"redis.cluster.nodes=" + containerIpAddress + ":" + mappedPort
					)
					.applyTo(configurableApplicationContext);
		}
	}

}
