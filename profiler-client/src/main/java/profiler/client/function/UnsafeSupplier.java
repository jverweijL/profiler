package profiler.client.function;

import javax.annotation.Generated;

/**
 * @author jverweij
 * @generated
 */
@FunctionalInterface
@Generated("")
public interface UnsafeSupplier<T, E extends Throwable> {

	public T get() throws E;

}