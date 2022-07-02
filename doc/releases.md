**TBD(to be determined)**

- Performance Tuning, Insert with es cluster environment 
[reference](https://www.elastic.co/blog/benchmarking-and-sizing-your-elasticsearch-cluster-for-logs-and-metrics)
- Performance Tuning, preprocesses according to the amount of data, 
performs segmentation according to time or id, 
parallel insertion by multi-threaded, divide and conquer
- Close the connection before closing the service? The new java client does not have close, the old rest client has close.
 [reference](https://discuss.elastic.co/t/closing-client-on-shutdown/14669/3)
- espipe logo
- distribution mode

**0.0.4-SNAPSHOT-TBD**
- data validation after init? Is it necessary? like 108500 was inserted but only 108492 is success.
- integrate with dynamic-tp to see what's the thread pool is going on
- The shutdown hook gracefully shuts down the program and waits for the current thread to finish executing before shutting down.
 If the record fails, it starts from the location where the record was executed by a thread.

**0.0.3-SNAPSHOT**
- move design to /doc/design.md
- move tbd abd release notes to /doc/releases.md
- add online api for usage
- tuning code structure as design 
- same index's init and sync should not run together at same time 
- unit test for espipe-core module

**0.0.2-SNAPSHOT**

- Prepare H2 data source for testing.
- Prepare H2 test environment and sample SQL scripts, integrate jdbc.
- Test H2 environment single-threaded data synchronization, integrate elasticsearch-java-client.
- Single thread takes 100k 2h 42m 33s 118ms.
- Performance tuning, increased jdbc fetch-size from 10 to 100, data loading time is decreased from 9s to 3.5s
- Performance tuning, es client uses bulk synchronous batch insert, reducing 2h to 8m 39s 924ms
- Performance tuning, es client uses bulk asynchronous batch insertion, reducing 8m 39s 924ms to 4m 4s 264ms
- Performance tuning, The amount of asynchronous bulk insert data is increased from 10 to 12,000 at a time, and the refresh_interval of the index is turned off, reducing 4m 4s 264ms to 15s 405ms
- Massive code refactoring
- Multithreading, One thread per index
- Promote on WeChat, recruit contributors


**0.0.1-SNAPSHOT**
     
- Integrate Source code management (SCM), try code deployment.     
- Integrate codestyle, try code specification.
- Release version 0.0.1.