package io.github.nooshhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sample worker is used to mimic creating data for testing
 * @author neals
 * @since 6/3/2022
 */
@Service
public class EspipeSampleWorker {
    private final static String INSERT = "insert into nh_project values (?, ?, ?, ?, ?)";
    
    @Value("${spring.profiles.active:h2}")
    private String profile;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // preserve id form 1 to 9 to manually create data
    private final AtomicInteger atomicInteger = new AtomicInteger(10);

    // TODO: schedule this to create data randomly
    public void create() {
        // ONLY create data for h2 database
        if(profile.equals("h2")) {
            List<Object[]> data = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                Object[] args = new Object[5];
                args[0] = atomicInteger.incrementAndGet();
                args[1] = "project " + args[0];
                args[2] = "1,2,3";
                args[3] = LocalDateTime.now();
                args[4] = null;
                data.add(args);
            }
            System.out.println("data from " + data.get(0)[0] + " is prepared");
            jdbcTemplate.batchUpdate(INSERT, data);
        }
    }

}
