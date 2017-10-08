package com.xsonus.ffttest

import org.jtransforms.fft.DoubleFFT_1D



/**
 * Created by F on 08/10/17.
 */
class FrequencyScanner {

    private var window: DoubleArray? = null


    /** extract the dominant frequency from 16bit PCM data.
     * @param sampleData an array containing the raw 16bit PCM data.
     * *
     * @param sampleRate the sample rate (in HZ) of sampleData
     * *
     * @return an approximation of the dominant frequency in sampleData
     */
    fun extractFrequency(sampleData: ShortArray, sampleRate: Int): Double {
        /* sampleData + zero padding */
        val fft = DoubleFFT_1D((sampleData.size + 24 * sampleData.size).toLong())
        val a = DoubleArray((sampleData.size + 24 * sampleData.size) * 2)

        System.arraycopy(applyWindow(sampleData), 0, a, 0, sampleData.size)
        fft.realForward(a)

        /* find the peak magnitude and it's index */
        var maxMag = java.lang.Double.NEGATIVE_INFINITY
        var maxInd = -1

        for (i in 0..a.size / 2 - 1) {
            val re = a[2 * i]
            val im = a[2 * i + 1]
            val mag = Math.sqrt(re * re + im * im)

            if (mag > maxMag) {
                maxMag = mag
                maxInd = i
            }
        }

        /* calculate the frequency */
        return sampleRate.toDouble() * maxInd / (a.size / 2)
    }

    /** build a Hamming window filter for samples of a given size
     * See http://www.labbookpages.co.uk/audio/firWindowing.html#windows
     * @param size the sample size for which the filter will be created
     */
    private fun buildHammWindow(size: Int) {
        if (window != null && window!!.size == size) {
            return
        }
        window = DoubleArray(size)
        for (i in 0..size - 1) {
            window!![i] = .54 - .46 * Math.cos(2.0 * Math.PI * i.toDouble() / (size - 1.0))
        }
    }

    /** apply a Hamming window filter to raw input data
     * @param input an array containing unfiltered input data
     * *
     * @return a double array containing the filtered data
     */
    private fun applyWindow(input: ShortArray): DoubleArray {
        val res = DoubleArray(input.size)

        buildHammWindow(input.size)
        for (i in input.indices) {
            res[i] = input[i].toDouble() * window!![i]
        }
        return res
    }
}