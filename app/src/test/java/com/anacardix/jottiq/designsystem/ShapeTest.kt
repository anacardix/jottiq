package com.anacardix.jottiq.designsystem

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private val DENSITY = Density(density = 1f)
private val ZERO_SIZE = Size.Zero

class ShapeTest {

    @Test
    fun `single row group gets the full radius on every corner`() {
        val shape = groupedRowShape(index = 0, count = 1)

        assertThat(shape.topStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(18f)
        assertThat(shape.topEnd.toPx(ZERO_SIZE, DENSITY)).isEqualTo(18f)
        assertThat(shape.bottomEnd.toPx(ZERO_SIZE, DENSITY)).isEqualTo(18f)
        assertThat(shape.bottomStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(18f)
    }

    @Test
    fun `first row of many gets the full radius on top and the inner radius on bottom`() {
        val shape = groupedRowShape(index = 0, count = 3)

        assertThat(shape.topStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(18f)
        assertThat(shape.bottomStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(5f)
    }

    @Test
    fun `middle row gets the inner radius on every corner`() {
        val shape = groupedRowShape(index = 1, count = 3)

        assertThat(shape.topStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(5f)
        assertThat(shape.bottomStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(5f)
    }

    @Test
    fun `last row of many gets the inner radius on top and the full radius on bottom`() {
        val shape = groupedRowShape(index = 2, count = 3)

        assertThat(shape.topStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(5f)
        assertThat(shape.bottomStart.toPx(ZERO_SIZE, DENSITY)).isEqualTo(18f)
    }
}
