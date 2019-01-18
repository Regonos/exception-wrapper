package pl.assecods.socrates.commons.exception;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.regonos.exception.wrapper.ExceptionWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionWrapperTest {

    private static final String GIVEN_STRING = "string";
//    Logger logger = LoggingFacade.getLogger(ExceptionWrapperTest.class);

    private List<Throwable> dummyList;

    @Before
    public void setup() {
        dummyList = new ArrayList<>();
    }

    @Test(expected = RuntimeException.class)
    public void thenRethrowFor_givenCheckedException_shouldRethrowForGivenException() {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        }).thenRethrowFor(RuntimeException::new, Exception.class);
    }


    @Test(expected = Exception.class)
    public void thenRethrowFor_givenUncheckedException_shouldRethrowForGivenException() throws Exception {
        ExceptionWrapper.handle(() -> {
            throw new RuntimeException();
        }).thenRethrowFor(Exception::new, RuntimeException.class);
    }


    @Test
    public void thenInvokeFor_givenChecked_exception_shouldExecuteGivenCode() {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        }).thenInvokeFor(dummyList::add, Exception.class);

        checkExceptionWrapperResult();
    }

    @Test
    public void thenInvokeFor_givenUncheckedException_shouldExecuteGivenCode() {
        ExceptionWrapper.handle(() -> {
            throw new RuntimeException();
        }).thenInvokeFor(dummyList::add, RuntimeException.class);

        checkExceptionWrapperResult();
    }

    @Test(expected = RuntimeException.class)
    public void handle_givenUncheckedException_shouldPassThroughUnhandledException() {

        ExceptionWrapper.handle(() -> {
            throw new RuntimeException();
        }).thenInvokeFor(ex -> Assert.fail(), Exception.class);

    }

    @Test
    public void handle_givenCheckedException_shouldSuppressExceptionIfUnhandled() {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        }).thenInvokeFor(ex -> Assert.fail("Should not execute"), RuntimeException.class);
    }

    @Test(expected = Exception.class)
    public void rethrowUnsafe_givenCheckedException_shouldThrowUnhandledCheckedExceptionAsUnchecked() {
        ExceptionWrapper
            .handle(() -> {
                throw new Exception();
            })
            .rethrowUnsafe()
            .thenInvokeFor(ex -> Assert.fail("Should not execute"), RuntimeException.class);
    }

    @Test(expected = RuntimeException.class)
    public void thenRethrowForUnhandled_givenCheckedException_shouldHandleAllUnhandledExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        })
            .andRethrowFor(IllegalArgumentException::new, RuntimeException.class)
            .thenRethrowForUnhandled(RuntimeException::new);
    }


    @Test(expected = Exception.class)
    public void thenRethrowForUnhandled_givenUncheckedException_shouldHandleAllUnhandledExceptions() throws Exception {
        ExceptionWrapper.handle(() -> {
            throw new RuntimeException();
        })
            .andRethrowFor(IllegalArgumentException::new, Exception.class)
            .thenRethrowForUnhandled(Exception::new);
    }

    @Test
    public void thenInvokeForUnhandled_givenCheckedException_shouldExecuteGivenCodeForAllUnhandledExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        })
            .andRethrowFor(IllegalArgumentException::new, RuntimeException.class)
            .thenInvokeForUnhandled(dummyList::add);

        checkExceptionWrapperResult();
    }


    @Test
    public void thenInvokeForUnhandled_givenUncheckedException_shouldExecuteGivenCodeForAllUnhandledExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new RuntimeException();
        })
            .andRethrowFor(IllegalArgumentException::new, Exception.class)
            .thenInvokeForUnhandled(dummyList::add);

        checkExceptionWrapperResult();
    }

    @Test(expected = Exception.class)
    public void thenRethrowForParent_givenUncheckedException_shouldRethrowForAllChildrenException() throws Exception {
        ExceptionWrapper.handle(() -> {
            throw new IllegalArgumentException();
        }).thenRethrowForParent(Exception::new, RuntimeException.class);
    }

    @Test(expected = RuntimeException.class)
    public void thenRethrowForParent_givenCheckedException_shouldRethrowForAllChildrenException() {
        ExceptionWrapper.handle(() -> {
            throw new IOException();
        }).thenRethrowForParent(RuntimeException::new, Exception.class);
    }

    @Test
    public void thenInvokeForParent_givenUncheckedException_shouldExecuteGivenCodeForAllChildExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new IllegalArgumentException();
        }).thenInvokeForParent(dummyList::add, RuntimeException.class);

        checkExceptionWrapperResult();
    }

    @Test
    public void thenInvokeForParent_givenCheckedException_shouldExecuteGivenCodeForAllChildExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new IOException();
        }).thenInvokeForParent(dummyList::add, Exception.class);

        checkExceptionWrapperResult();
    }


    @Test
    public void thenInvokeFor_givenCorrectValue_shouldReturnGivenValueIfExceptionNotThrown() {
        String result = ExceptionWrapper.handle(() -> GIVEN_STRING)
            .thenInvokeFor(ex -> {
            }, Exception.class);
        assertThat(result).isEqualTo(GIVEN_STRING);
    }

    @Test
    public void thenInvokeForParent_givenCorrectValue_shouldReturnGivenValueIfExceptionNotThrown() {
        String result = ExceptionWrapper.handle(() -> GIVEN_STRING)
            .thenInvokeForParent(ex -> {
            }, Exception.class);
        assertThat(result).isEqualTo(GIVEN_STRING);
    }

    @Test
    public void thenInvokeForUnhandled_givenCorrectValue_shouldReturnGivenValueIfExceptionNotThrown() {
        String result = ExceptionWrapper.handle(() -> GIVEN_STRING)
            .thenInvokeForUnhandled(ex -> {
            });
        assertThat(result).isEqualTo(GIVEN_STRING);
    }

    @Test
    public void thenRethrowFor_givenCorrectValue_shouldReturnGivenValueIfExceptionNotThrown() {
        String result = ExceptionWrapper.handle(() -> GIVEN_STRING)
            .thenRethrowFor(RuntimeException::new, Exception.class);
        assertThat(result).isEqualTo(GIVEN_STRING);
    }

    @Test
    public void thenRethrowForParent_givenCorrectValue_shouldReturnGivenValueIfExceptionNotThrown() {
        String result = ExceptionWrapper.handle(() -> GIVEN_STRING)
            .thenRethrowForParent(RuntimeException::new, Exception.class);
        assertThat(result).isEqualTo(GIVEN_STRING);
    }

    @Test
    public void thenRethrowForUnhandled_givenCorrectValue_shouldReturnGivenValueIfExceptionNotThrown() {
        String result = ExceptionWrapper.handle(() -> GIVEN_STRING)
            .thenRethrowForUnhandled(RuntimeException::new);
        assertThat(result).isEqualTo(GIVEN_STRING);
    }

    @Test(expected = RuntimeException.class)
    public void andRethrowFor_givenCheckedException_shouldRethrowForGivenException() {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        }).andRethrowFor(RuntimeException::new, Exception.class);
    }

    @Test(expected = Exception.class)
    public void andRethrowFor_givenUncheckedException_shouldRethrowForGivenException() throws Exception {
        ExceptionWrapper.handle(() -> {
            throw new RuntimeException();
        }).andRethrowFor(Exception::new, RuntimeException.class);
    }

    @Test(expected = RuntimeException.class)
    public void andRethrowForParent_givenCheckedException_shouldRethrowAllChildExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new IOException();
        }).andRethrowForParent(RuntimeException::new, Exception.class);
    }

    @Test(expected = Exception.class)
    public void andRethrowForParent_givenUncheckedException_shouldRethrowAllChildExceptions() throws Exception {
        ExceptionWrapper.handle(() -> {
            throw new IllegalArgumentException();
        }).andRethrowForParent(Exception::new, RuntimeException.class);
    }

    @Test
    public void andInvokeFor_givenCheckedException_shouldExecuteCodeForGivenException() {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        }).andInvokeFor(dummyList::add, Exception.class);

        checkExceptionWrapperResult();
    }

    @Test
    public void andInvokeFor_givenUncheckedException_shouldExecuteCodeForGivenException() {
        ExceptionWrapper.handle(() -> {
            throw new RuntimeException();
        }).andInvokeFor(dummyList::add, RuntimeException.class);

        checkExceptionWrapperResult();
    }

    @Test
    public void andInvokeForParent_givenCheckedException_shouldExecuteCodeForAllChildExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new IOException();
        }).andInvokeForParent(dummyList::add, Exception.class);

        checkExceptionWrapperResult();
    }

    @Test
    public void andInvokeForParent_givenUncheckedException_shouldExecuteCodeForAllChildExceptions() {
        ExceptionWrapper.handle(() -> {
            throw new IllegalArgumentException();
        }).andInvokeForParent(dummyList::add, RuntimeException.class);

        checkExceptionWrapperResult();
    }

    @Test(expected = Exception.class)
    public void andThrowFor_givenCheckedException_shouldRethrowTheSameException() throws Exception {
        ExceptionWrapper.handle(() -> {
            throw new Exception();
        }).andThrowFor(Exception.class);
    }

    private void checkExceptionWrapperResult() {
        if (dummyList.isEmpty()) {
            Assert.fail();
        }
    }

}
