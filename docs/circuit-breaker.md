`DualConnection` is an implementation of `Connection` that can switch between connections provided
by `ConnectionProvider`. Creation of DualConnection is guarded by `CircuitBreaker#canCreateDualConnection` method.
If the circuit breaker doesn't allow to create a new `DualConnection`, the `DualConnection#Builder` will return main
the connection provided by `ConnectionProvider`.

Every call to the database goes through `DualCall` implementation. `DualCall` is responsible for executing the call
by invoking `SqlCall#call`. `DualCall` can affect `CircuitBreaker` state while handling the exceptions.



//TODO add links to API/SPI classes.
