package tech.cassandre.trading.bot.test.core.ta4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.ta4j.core.Bar;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import tech.cassandre.trading.bot.util.ta4j.DurationBarAggregator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationBarAggregatorTest {

    /** Date time formatter. */
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    /** Aggregator. */
    DurationBarAggregator aggregator;

    /** Test subscriber. */
    TestSubscriber testSubscriber;

    @Test
    @DisplayName("Check intra bar aggregation")
    public void shouldAggregateBars() {
        aggregator.update(getTime("2021-01-01 10:00:00"), 10);
        aggregator.update(getTime("2021-01-01 10:01:00"), 3);
        aggregator.update(getTime("2021-01-01 10:02:00"), 15);
        aggregator.update(getTime("2021-01-01 10:05:00"), 20);
        aggregator.update(getTime("2021-01-01 10:05:30"), 21);
        aggregator.update(getTime("2021-01-01 10:06:00"), 19);
        aggregator.update(getTime("2021-01-01 10:08:00"), 18);
        aggregator.update(getTime("2021-01-01 10:10:00"), 17);


        assertTrue(testSubscriber.subscribed);
        testSubscriber.request(1);

        assertEquals(2, testSubscriber.bars.size());

        assertEquals(10d, testSubscriber.bars.get(0).getOpenPrice().doubleValue());
        assertEquals(15d, testSubscriber.bars.get(0).getHighPrice().doubleValue());
        assertEquals(3d, testSubscriber.bars.get(0).getLowPrice().doubleValue());
        assertEquals(15d, testSubscriber.bars.get(0).getClosePrice().doubleValue());

        assertEquals(20d, testSubscriber.bars.get(1).getOpenPrice().doubleValue());
        assertEquals(21d, testSubscriber.bars.get(1).getHighPrice().doubleValue());
        assertEquals(18d, testSubscriber.bars.get(1).getLowPrice().doubleValue());
        assertEquals(18d, testSubscriber.bars.get(1).getClosePrice().doubleValue());

    }

    @BeforeEach
    public void setup(){
        aggregator = new DurationBarAggregator(Duration.ofMinutes(5));
        final Flux<Bar> barFlux = aggregator.getBarFlux();
        testSubscriber = new TestSubscriber();
        barFlux.subscribe(testSubscriber);
    }

    @Test
    @DisplayName("Check that aggregation does not happen, when time between bars is equal to last timestamp + distance")
    public void shouldNotAggregateBars() {
        aggregator.update(getTime("2021-01-01 00:00:00"), 10);
        aggregator.update(getTime("2021-01-01 00:05:00"), 3);
        aggregator.update(getTime("2021-01-01 00:06:00"), 5);
        aggregator.update(getTime("2021-01-01 00:07:00"), 2);
        aggregator.update(getTime("2021-01-01 00:10:00"), 15);
        aggregator.update(getTime("2021-01-01 00:15:00"), 20);

        assertTrue(testSubscriber.subscribed);
        testSubscriber.request(3);

        assertEquals(3, testSubscriber.bars.size());

        assertEquals(10d, testSubscriber.bars.get(0).getHighPrice().doubleValue());
        assertEquals(10d, testSubscriber.bars.get(0).getLowPrice().doubleValue());
        assertEquals(10d, testSubscriber.bars.get(0).getClosePrice().doubleValue());
        assertEquals(10d, testSubscriber.bars.get(0).getOpenPrice().doubleValue());

        assertEquals(5d, testSubscriber.bars.get(1).getHighPrice().doubleValue());
        assertEquals(2d, testSubscriber.bars.get(1).getLowPrice().doubleValue());
        assertEquals(2d, testSubscriber.bars.get(1).getClosePrice().doubleValue());
        assertEquals(3d, testSubscriber.bars.get(1).getOpenPrice().doubleValue());

        assertEquals(15d, testSubscriber.bars.get(2).getHighPrice().doubleValue());
        assertEquals(15d, testSubscriber.bars.get(2).getLowPrice().doubleValue());
        assertEquals(15d, testSubscriber.bars.get(2).getClosePrice().doubleValue());
        assertEquals(15d, testSubscriber.bars.get(2).getOpenPrice().doubleValue());

    }

    ZonedDateTime getTime(String value) {
        return LocalDateTime.parse(value, dateTimeFormatter).atZone(ZoneId.systemDefault());
    }

    private static class TestSubscriber extends BaseSubscriber<Bar> {
        boolean subscribed;
        final List<Bar> bars = new ArrayList<>();


        @Override
        protected void hookOnNext(Bar value) {
            super.hookOnNext(value);
            bars.add(value);
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            super.hookOnSubscribe(subscription);
            subscribed = true;
        }
    }
    
}
