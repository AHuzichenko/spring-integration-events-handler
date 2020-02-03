package ua.ardas.esputnik.events.domain;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import lombok.Getter;

@Getter
public enum StrategyInterval {

	HOURS(1, ChronoUnit.HOURS),
	DAYS(2, ChronoUnit.DAYS),
	WEEKS(3, ChronoUnit.WEEKS),
	MONTHS(4, ChronoUnit.MONTHS);

	private final int code;
	private final ChronoUnit interval;

	StrategyInterval(int code, ChronoUnit interval) {
		this.code = code;
		this.interval = interval;
	}

	public static ChronoUnit getIntervalByCode(int code) {
		return Arrays.stream(values()).filter(value -> value.code == code).map(StrategyInterval::getInterval).findFirst()
				.orElse(ChronoUnit.HOURS);

	}

}