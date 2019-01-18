package pl.regonos.exception.wrapper;

/**
 * Supplier that can throw a checked exception.
 *
 * @param <T>  Any object that will be returned from supplier.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Throwable;
}
