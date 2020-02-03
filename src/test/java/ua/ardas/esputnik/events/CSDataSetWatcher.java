package ua.ardas.esputnik.events;

import org.junit.rules.TestWatcher;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CSDataSetWatcher extends TestWatcher {
	private static final String KEY_SPACES = "events";

	public static void ensureKeyspaceAndSession(Cluster cluster) {
		Session session = cluster.connect();
		for (String keyspace : KEY_SPACES.split(",")) {
			try {
				String cql = "CREATE KEYSPACE " + keyspace + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};";
				log.info(cql);
				session.execute(cql);
			}
			catch (Exception e) {
				log.warn("Failed to create {}", keyspace);
			}
		}
	}
}

