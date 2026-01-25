package pl.edu.agh.to.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StopNormalizerTest {

    @Test
    void shouldNormalizeValidStop() {
        // given
        String input = " \t   \n Kawiory  \t \n";

        // when
        String result = StopNormalizer.normalize(input);

        // then
        assertThat(result).isEqualTo("Kawiory");
    }

    @Test
    void shouldReturnNull_givenOnlyWhitespace() {
        // given
        String input = " \t   \n  ";

        // when
        String result = StopNormalizer.normalize(input);

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNull_givenNull() {
        // given
        String input = null;

        // when
        String result = StopNormalizer.normalize(input);

        // then
        assertThat(result).isNull();
    }
}