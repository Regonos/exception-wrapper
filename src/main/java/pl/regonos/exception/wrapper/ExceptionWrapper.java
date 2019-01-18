package pl.regonos.exception.wrapper;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Builder class for complex exception handling. Look for method java docs to find a detailed description.
 * It can handle exception in multiple ways, including a complex method chaining with specified exception types.
 *
 * @author Igor Maculewicz
 */
//TODO add Optional "map-like" method to map exception to another object(not exception).
public class ExceptionWrapper<T> {

    private Logger logger = LoggerFactory.getLogger(ExceptionWrapper.class);

    private T result;
    private Throwable error;
    private Set<Class> usedExceptions;
    private boolean rethrowUnsafe;

    /**
     * Private constructor because of declared static one.
     *
     * @param supplier which contains code that have to be handled.
     */
    private ExceptionWrapper(@NonNull ThrowingSupplier<T> supplier, boolean rethrowUnsafe) {
        this.usedExceptions = new HashSet<>();
        this.rethrowUnsafe = rethrowUnsafe;

        handleSupplier(supplier);
    }

    /**
     * Main handler method which taking a supplier with code to handle.
     *
     * @param supplier which contains code that have to be handled.
     * @param <T>      every object which will be returned later in process.
     * @return instance of created {@link ExceptionWrapper}.
     */
    public static <T> ExceptionWrapper<T> handle(@NonNull ThrowingSupplier<T> supplier) {
        return new ExceptionWrapper<>(supplier, false);
    }

    /**
     * If expected exception is thrown, propagate it higher.
     *
     * @param expectingException exception which will be propagated
     * @param <X>                Generic exception type which will be thrown.
     * @return instance of created {@link ExceptionWrapper}.
     * @throws X {@link Throwable} which will be thrown on fail in supplier.
     */
    @SuppressWarnings("unchecked")
    public <X extends Throwable> ExceptionWrapper<T> andThrowFor(@NonNull Class<X> expectingException) throws X {

        if (checkAnyClassIsAllowed(true, expectingException)) {
            usedExceptions.add(expectingException);
            throw (X) error;
        }

        return this;
    }

    /**
     * Method that will rethrow to given exception, only when one of exception on given list will be thrown while execution of supplier.
     *
     * @param rethrowMethod       method which will rethrow to given exception, if one of exception will be thrown in supplier.
     * @param expectingExceptions list of expected exceptions.
     * @param <X>                 {@link Throwable} which will be thrown on fail in supplier.
     * @return instance of {@link ExceptionWrapper} to further chaining.
     * @throws X {@link Throwable} which will be thrown on fail in supplier.
     */
    public <X extends Throwable> ExceptionWrapper<T> andRethrowFor(@NonNull Function<Throwable, X> rethrowMethod, @NonNull Class... expectingExceptions) throws X {

        if (checkAnyClassIsAllowed(true, expectingExceptions)) {
            throw rethrowMethod.apply(error);
        }

        return this;
    }

    /**
     * Finalizer method that will rethrow to given exception, only when one of exception on given list will be thrown while execution of supplier.
     *
     * @param rethrowMethod       method which will rethrow to given exception, if one of exception will be thrown in supplier.
     * @param expectingExceptions list of expected exceptions.
     * @param <X>                 {@link Throwable} which will be thrown on fail in supplier.
     * @return returns <T> value if no exception thrown in supplier.
     * @throws X {@link Throwable} which will be thrown on fail in supplier.
     */
    public <X extends Throwable> T thenRethrowFor(@NonNull Function<Throwable, X> rethrowMethod, @NonNull Class... expectingExceptions) throws X {

        boolean isError =
                //If expecting exceptions is empty
                (expectingExceptions.length == 0
                        //And error is present
                        && Objects.nonNull(error))
                        //Or Any of given classes are allowed
                        || checkAnyClassIsAllowed(true, expectingExceptions);

        if (isError) {
            throw rethrowMethod.apply(error);
        }

        escapeUnhandledRuntimeExceptions();

        return result;
    }


    /**
     * Method that will rethrow to given exception, for all children exceptions of given exception.
     *
     * @param rethrowMethod      method which will rethrow to given exception, if one of exception will be thrown in supplier.
     * @param expectingException parent exception.
     * @return instance of {@link ExceptionWrapper} to further chaining.
     * @throws X {@link Throwable} which will be thrown on fail in supplier.
     */
    public <X extends Throwable> ExceptionWrapper<T> andRethrowForParent(@NonNull Function<Throwable, X> rethrowMethod, @NonNull Class expectingException) throws X {

        if (Objects.nonNull(error) && isChildrenOf(error.getClass(), expectingException) && !usedExceptions.contains(error.getClass())) {
            usedExceptions.add(error.getClass());
            throw rethrowMethod.apply(error);
        }

        return this;
    }

    /**
     * Finalizer method that will rethrow to given exception, for all children exceptions of given exception.
     *
     * @param rethrowMethod      method which will rethrow to given exception, if one of exception will be thrown in supplier.
     * @param expectingException parent exception.
     * @return returns <T> value if no exception thrown in supplier.
     * @throws X {@link Throwable} which will be thrown on fail in supplier.
     */
    public <X extends Throwable> T thenRethrowForParent(@NonNull Function<Throwable, X> rethrowMethod, @NonNull Class expectingException) throws X {

        if (Objects.nonNull(error) && isChildrenOf(error.getClass(), expectingException) && !usedExceptions.contains(error.getClass())) {
            usedExceptions.add(error.getClass());
            throw rethrowMethod.apply(error);
        }

        escapeUnhandledRuntimeExceptions();

        return result;
    }

    /**
     * Finalizer method that will rethrow to given exception, only if it wasn't handled before in chain.
     *
     * @param exceptionSupplier function that returns an exception which will be used in rethrow.
     * @param <X>               any {@link Throwable}.
     * @return returns <T> value if no exception thrown in supplier.
     * @throws X any {@link Throwable}.
     */
    public <X extends Throwable> T thenRethrowForUnhandled(@NonNull Function<Throwable, X> exceptionSupplier) throws X {

        if (Objects.nonNull(error) && isUnhandled(error.getClass())) {
            throw exceptionSupplier.apply(error);
        }

        return result;
    }

    /**
     * Method that will invoke given method only when one of given exception list will be thrown while execution of supplier.
     *
     * @param errorConsumer       method which will be invoked if one of exception will be thrown in supplier.
     * @param expectingExceptions list of expected exceptions. If empty then will be treated as any {@link Throwable}
     * @return instance of {@link ExceptionWrapper} to further chaining.
     */
    public ExceptionWrapper<T> andInvokeFor(@NonNull Consumer<Throwable> errorConsumer, @NonNull Class... expectingExceptions) {

        if (checkAnyClassIsAllowed(false, expectingExceptions)) {
            errorConsumer.accept(error);
        }

        return this;
    }

    /**
     * Finalizer method that will invoke given method only when one of given exception list will be thrown while execution of supplier.
     *
     * @param errorConsumer       method which will be invoked if one of exception will be thrown in supplier.
     * @param expectingExceptions list of expected exceptions. If empty then will be treated as any {@link Throwable}
     * @return returns <T> value if no exception thrown in supplier.
     */
    public T thenInvokeFor(@NonNull Consumer<Throwable> errorConsumer, @NonNull Class... expectingExceptions) {

        boolean isError =
                //If expecting exceptions is empty
                (expectingExceptions.length == 0
                        //And error is present
                        && Objects.nonNull(error))
                        //Or Any of given classes are allowed
                        || checkAnyClassIsAllowed(true, expectingExceptions);

        if (isError) {
            errorConsumer.accept(error);
        } else {
            escapeUnhandledRuntimeExceptions();
        }

        return result;
    }

    /**
     * Method that will invoke given method, for all children exceptions of given exception.
     *
     * @param errorConsumer      method which will be invoked if one of exception will be thrown in supplier.
     * @param expectingException parent exception.
     * @return instance of {@link ExceptionWrapper} to further chaining.
     */
    public ExceptionWrapper<T> andInvokeForParent(@NonNull Consumer<Throwable> errorConsumer, @NonNull Class expectingException) {

        if (Objects.nonNull(error) && isChildrenOf(error.getClass(), expectingException) && !usedExceptions.contains(error.getClass())) {
            errorConsumer.accept(error);
        }

        return this;
    }

    /**
     * Finalizer method that will invoke given method, for all children exceptions of given exception.
     *
     * @param errorConsumer      method which will be invoked if one of exception will be thrown in supplier.
     * @param expectingException parent exception.
     * @return returns <T> value if no exception thrown in supplier.
     */
    public T thenInvokeForParent(@NonNull Consumer<Throwable> errorConsumer, @NonNull Class expectingException) {

        if (Objects.nonNull(error) && isChildrenOf(error.getClass(), expectingException) && !usedExceptions.contains(error.getClass())) {
            errorConsumer.accept(error);
        } else {
            escapeUnhandledRuntimeExceptions();
        }


        return result;
    }

    /**
     * Finalizer method that will invoke given method, only if it wasn't handled before in chain.
     *
     * @param errorConsumer method which will be invoked if one of exception will be thrown in supplier.
     * @return returns <T> value if no exception thrown in supplier.
     */
    public T thenInvokeForUnhandled(@NonNull Consumer<Throwable> errorConsumer) {

        if (Objects.nonNull(error) && isUnhandled(error.getClass())) {
            errorConsumer.accept(error);
        }

        return result;
    }

    /**
     * Method to allow throwing unchecked exceptions as checked in finalizer methods.
     *
     * @return instance of {@link ExceptionWrapper} to further chaining.
     */
    public ExceptionWrapper<T> rethrowUnsafe() {
        this.rethrowUnsafe = true;
        return this;
    }

    /**
     * Main method to handle a code in supplier.
     *
     * @param supplier supplier with code to execute.
     */
    private void handleSupplier(ThrowingSupplier<T> supplier) {
        try {
            this.result = supplier.get();
        } catch (Throwable ex) {
            this.error = ex;
        }
    }

    /**
     * Check that given classes is allowed to use in given method.
     *
     * @param registerExceptionAsUsed register given classes as used.
     * @param classes                 classes which have to be checked.
     * @return flag that classes are allowed or not.
     */
    private boolean checkAnyClassIsAllowed(boolean registerExceptionAsUsed, Class... classes) {
        checkAlreadyUsed(classes);

        if (registerExceptionAsUsed) {
            usedExceptions.addAll(Arrays.asList(classes));
        }

        return Objects.nonNull(error) &&
                Stream.of(classes).anyMatch(cl -> error.getClass().equals(cl));
    }

    /**
     * Check that given classes was already used.
     *
     * @param classes classes which have to be checked.
     */
    private void checkAlreadyUsed(Class... classes) {
        Stream.of(classes).forEach(v -> {
            if (usedExceptions.contains(v)) {
                logger.warn("You handled exception: {}, more than once!", v.getName());
            }
        });
    }

    /**
     * Checks that class is unhandled.
     *
     * @param unhandledClass class which have to be checked.
     * @return flag that classes are unhandled or not.
     */
    private boolean isUnhandled(Class unhandledClass) {
        return usedExceptions.stream().noneMatch(ex -> ex.equals(unhandledClass));
    }

    /**
     * Check that given class is children of given parent class.
     *
     * @param childrenClass children class which will be checked.
     * @param parentClass   parent class which will be checked.
     * @return flag that indicates that given children class is children of given parent class.
     */
    private boolean isChildrenOf(Class childrenClass, Class parentClass) {
        return !childrenClass.equals(parentClass) && parentClass.isAssignableFrom(childrenClass);
    }

    /**
     * Escape unhandled exceptions.
     */
    private void escapeUnhandledRuntimeExceptions() {
        boolean allowEscape = (this.rethrowUnsafe || error instanceof RuntimeException)
                && checkAnyClassIsAllowed(false, error.getClass());

        if (allowEscape) {
            throw escapeRuntimeException(error);
        }
    }

    /**
     * Cast a CheckedException as an unchecked one.
     *
     * @param throwable to cast
     * @param <X>       the type of the Throwable
     * @return this method will never return a Throwable instance, it will just throw it.
     * @throws T the throwable as an unchecked throwable
     */
    @SuppressWarnings("unchecked")
    private <X extends Throwable> RuntimeException escapeRuntimeException(Throwable throwable) throws X {
        throw (X) throwable;
    }
}
