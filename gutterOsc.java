/*-------------------------------------------------------------------*
 * Forced damped Duffing system coupled to a bank of resonators (band-pass biquad filters)
 * Biquad code from http://www.musicdsp.org/files/biquad.c
 *-------------------------------------------------------------------*/
import com.cycling74.max.*;
import com.cycling74.msp.*;

public class gutterOsc extends MSPPerformer
{
	/*-------------------------------------------------------------------*
	 * value will store our current x value.
	 * r is the logistic map's r parameter.
	 *-------------------------------------------------------------------*/
	public double[][] a0, a1, a2, b1, b2;
	
	public double[][] filterFreqsArray, filterFreqsArrayTemp;
	//public double[] filterFreqsTempArray = {68, 97, 170, 248, 391, 449, 531, 589, 658, 711, 879, 771, 807, 1053, 1200, 1255, 1460, 1478, 1521, 1685, 1666, 1784, 1921, 1954, 68, 97, 170, 248, 391, 449, 531, 589, 658, 711, 879, 771, 807, 1053, 1200, 1255, 1460, 1478, 1521, 1685, 1666, 1784, 1921, 1954};
	public double[] Q, QTemp;
	//public double[] QTemp = {30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30};
	public double[][] V, K, norm, prevX1, prevX2, prevY1, prevY2, y;
	public double[] gains;
	public double M_PI, Fs, singleGain;
	public double audioInput;
	public boolean enableAudioInput;
	public int bankCount = 2;

	public double smoothing; // for the lowpass. 1 = no lowpass, 5 = quite lowpassed
	
	public double duffX, duffY, dx, dy;
	public double gamma, omega, c, t, dt;
	
	public float[] x;
	public double finalY;

	public int distMethod = 2;
	
	public int filterCount;

	public boolean filtersOn;

	public gutterOsc()
	{
		/*-------------------------------------------------------------------*
		 * No inlets, one audio-rate outlet.
		 *-------------------------------------------------------------------*/
		declareInlets(new int[]  {SIGNAL, SIGNAL, SIGNAL, SIGNAL, SIGNAL, SIGNAL, SIGNAL, SIGNAL}); // gamma, sigma, c, dt, gain, AUDIO
		declareOutlets(new int[] { SIGNAL, SIGNAL, DataTypes.ANYTHING });
		
		M_PI = 3.14159265358979323846;	
		Fs = 44100;		
		filterCount = 24;
		filtersOn = true;	// turn off for Duffing only
		singleGain = 0.0;

		enableAudioInput = false;   // uses the sine forcing for the Duffing by default

		smoothing = 1; // for the lowpass. 1 = no lowpass, 5 = quite lowpassed

		// TO KEEP after having moved all this to the init functions at the bottom:
		initMainArrays(bankCount, filterCount);
		
		gains = new double[bankCount];
		
		for (int i=0; i<bankCount; i++) {
			if (i==0) { gains[i] = 1;} else {gains[i] = 0;}	
			for (int j=0; j<filterCount; j++) { 	
				filterFreqsArray[i][j] = (j/2.0)*20 * (i+1)*1.2 + 80;		// INIT arbitrary filter freqs
				y[i][j] = 0;	prevX1[i][j] = 0; prevX2[i][j] = 0; prevY1[i][j] = 0; prevY2[i][j] = 0;
			}
		}

		initTempArrays(bankCount, filterCount);
		for (int j=0; j<filterCount; j++) {	Q[j] = 30; QTemp[j] = 30;	}	

		calcCoeffs();
		
		duffX = 0; duffY = 0; dx = 0; dy = 0;
		gamma = 0.1; omega = 1.25; c = 0.3;
		t = 0;
		dt = 1;
		

		
	}
	
	public void setFreqNM(int BANK, int FILTER, float FREQ) {  
		if(FILTER < filterCount) { filterFreqsArray[BANK][FILTER] = FREQ; filterFreqsArrayTemp[BANK][FILTER] = FREQ;  calcCoeffs();  }
	}
	public void setQN(int FILTER, float newQ) { if(FILTER < filterCount) { Q[FILTER] = newQ; QTemp[FILTER] = newQ; calcCoeffs(); }  }
	public void setGainN(int BANK, float GAIN) { gains[BANK] = GAIN; }
	public void setLowpass(float newSmooth) { smoothing = newSmooth;} // for lowpass filter
	public void setSingleGain(float GAIN) { singleGain = GAIN; }
	public void toggleFilters(int ON) { if (ON <= 0) { filtersOn = false;} else {filtersOn = true;} }
	public void setDistortionMethod(int METHOD) { distMethod = METHOD; }
	public void toggleAudioInput(int ENABLE) { if (ENABLE==1) {enableAudioInput = true;} else { enableAudioInput = false;}  }

	public void filters(int NUM ) { 
		int oldFilterCount = filterCount;
		filterCount = NUM;

		initMainArrays(bankCount, filterCount);			// see the bottom - doesn't include temp arrays

		for (int i=0; i<bankCount; i++) {
			for (int j=0; j< Math.min(filterCount, oldFilterCount); j++) {				// fill in with temp array values up to min(old, new)
				filterFreqsArray[i][j] = filterFreqsArrayTemp[i][j];
				Q[j] = QTemp[j];
			}
			if (filterCount > oldFilterCount) {												
				for (int j=oldFilterCount; j<filterCount; j++) {						// if the new filterCount is bigger than the old
					filterFreqsArray[i][j] = filterFreqsArray[i][oldFilterCount-1];		// set any new extra filters to the last filter freq 
					Q[j] = Q[oldFilterCount-1];
				}
			}
		}

		initTempArrays(bankCount, filterCount);
		calcCoeffs();
	}

	
	private void calcCoeffs() {						// REFRESH FILTER COEFFS (WHEN FILTER FREQS, QS ARE UPDATED)
		for (int i=0; i<bankCount; i++) {							// for each bank
			for (int j=0; j<filterCount; j++) { 					// for each filter in each bank
				V[i][j] = Math.pow(10, 1.0 / 20);		// NOTE gains[i] removed from here!! It is applied later!!!
				K[i][j] = Math.tan(M_PI * filterFreqsArray[i][j] / Fs);
				norm[i][j] = 1 / (1 + K[i][j] / Q[j] + K[i][j] * K[i][j]);
				a0[i][j] = K[i][j] / Q[i] * norm[i][j];
				a1[i][j] = 0;
				a2[i][j] = -a0[i][j];
				b1[i][j] = 2 * (K[i][j] * K[i][j] - 1) * norm[i][j];
				b2[i][j] = (1 - K[i][j] / Q[i] + K[i][j] * K[i][j]) * norm[i][j];  	
			}
		}
	}



	public void resetDuff() {this.duffX = 0; this.duffY = 0; this.dx = 0; this.dy = 0; this.t = 0;}
	
	public void reset(){ 
		this.duffX = 0; this.duffY = 0; this.dx = 0; this.dy = 0; this.t = 0; this.dt=0;
		for (int i=0; i<bankCount; i++) {
			for (int j = 0; j<filterCount; j++) {
				y[i][j] = 0;	prevX1[i][j] = 0; prevX2[i][j] = 0; prevY1[i][j] = 0; prevY2[i][j] = 0;
			}
		}
	}

	public double distortion(double d_inc, int d_mode) {
		// d_inc is the temporary final output
		double returnDuffX = 0.0;
				if (d_mode==0) {			// DIST #0: 	Clipping
					returnDuffX = Math.max(Math.min(d_inc, 1), -1);
				} 
				else if (d_mode==1) {		// DIST #1: 	??? cubic with clipping? Can't remember where this came from
					if (finalY <= -1) { returnDuffX = -0.666666667;}
					else if (d_inc <= 1) {returnDuffX = d_inc - (d_inc*d_inc*d_inc)/3;}
					else {returnDuffX = 0.666666667; }
				} 
				else if (d_mode==2) {		// DIST #2: 	tanh
					returnDuffX = Math.atan(d_inc);	
				}
				else if (d_mode==3) {		// DIST #3: 	atan approximation? From http://www.kvraudio.com/forum/viewtopic.php?p=4402120
					returnDuffX = 0.75*(Math.sqrt(((d_inc*1.3)*(d_inc*1.3)+1))*1.65-1.65)/d_inc;	
				}
				else if (d_mode==4) {		// DIST #4: 	tanh approximation? From http://www.kvraudio.com/forum/viewtopic.php?p=4402120
					returnDuffX = (0.1076*d_inc*d_inc*d_inc + 3.029*d_inc)/(d_inc*d_inc + 3.124);	
				}
				else if (d_mode==5) {		// DIST #5: 	sigmoid function - see Kiefer and the ESN tutorial
					returnDuffX = 2 / (1 + Math.exp(-1 * d_inc));	 // modified to increase the gain (2 instead of 1)
				}
		return returnDuffX;
	}

	public double lowpass(double new_val, double old_val, double smooth) {
		return (new_val - old_val) / smooth;
	}

	public void dspsetup(MSPSignal[] in , MSPSignal[] out)
	{		// Called when the object's audio processing is switched on.
		for (int i=0; i<bankCount; i++) {
			 for (int j=0; j<filterCount; j++) {
			 	this.y[i][j] = 0;		this.prevX1[i][j] = 0; this.prevX2[i][j] = 0; this.prevY1[i][j] = 0; this.prevY2[i][j] = 0;
			 }
		}	
	}
	
	public void perform(MSPSignal[] ins, MSPSignal[] outs) 
	{
		/*-------------------------------------------------------------------*
		 * Get a pointer to the arrays of output samples...
		 *-------------------------------------------------------------------*/
		 float[] out = outs[0].vec;
		 float[] out2 = outs[1].vec;

		/*-------------------------------------------------------------------*
		 * Go through the required number of samples
		 *-------------------------------------------------------------------*/
		for (int i = 0; i < out.length; i++)
		{
		
			// audio inputs to the mxj object
			gamma = ins[0].vec[i];  omega = ins[1].vec[i]; c = ins[2].vec[i]; dt = ins[3].vec[i]; singleGain = ins[4].vec[i];
			audioInput = ins[5].vec[i];
			gains[0] = ins[6].vec[i];			// gains need to be signal inputs...
			gains[1] = ins[7].vec[i];			// Note that this fixes the max at two for the moment.
	
			// band pass filters
			finalY = 0;
			if (filtersOn) {
				for (int j=0; j<bankCount; j++) {
					for (int k=0; k<filterCount; k++) { 
						y[j][k] = a0[j][k]*duffX  +  a1[j][k]*prevX1[j][k]  +  a2[j][k]*prevX2[j][k]  -  b1[j][k]*prevY1[j][k]  -  b2[j][k]*prevY2[j][k];
						prevX2[j][k] = prevX1[j][k];
						prevX1[j][k] = duffX;
						prevY2[j][k] = prevY1[j][k];
						prevY1[j][k] = y[j][k];
						finalY += y[j][k] * gains[j] * singleGain;			// retain singleGain for overall control
					}
				}
			} else {	// if filters are disabled then pass directly
				finalY = duffX;
			}

			// DUFFING with audioInput or with OSC?
			if (enableAudioInput) {  dy = finalY - (finalY*finalY*finalY) - (c*duffY) + gamma*audioInput;   		 }
			else {   				 dy = finalY - (finalY*finalY*finalY) - (c*duffY) + gamma*Math.sin(omega * t);	 }

			duffY += dy;
			dx = duffY;
			duffX = lowpass(finalY+dx, duffX, smoothing);

			if (filtersOn) {		// If the filters are enabled use variable distortion (function above)
				duffX = distortion(duffX, distMethod); 
				out[i] = (float) (finalY*0.125);  // output the value from the filter, not from the distortion
			} 
			else {			// If the filters are OFF use reset() function to reignite process (snazzy clicks?)
				duffX = Math.max(Math.min(duffX, 100), -100);
				if (Math.abs(duffX) > 99) {resetDuff();}
				out[i] = (float) (Math.max(Math.min(duffX*singleGain, 1), -1));
			}

			
			out2[i] = (float) (duffX);

			t += dt;	

			if (Double.isNaN(duffX)) { resetDuff();}
			
		}
		
	}

	private void initMainArrays(int bankC, int filterC) {

		filterFreqsArray = new double[bankC][];								
		prevX1 = new double[bankC][];	prevX2 = new double[bankC][];
		prevY1 = new double[bankC][];	prevY2 = new double[bankC][];
		V = new double[bankC][];		K = new double[bankC][];		norm = new double[bankC][];
		a0 = new double[bankC][];		a1 = new double[bankC][];		a2 = new double[bankC][];
		b1 = new double[bankC][];		b2 = new double[bankC][];
		y = new double[bankC][];

		Q = new double[filterC];						// FILTERCOUNT one Q for each filter

		for (int i=0; i<bankC; i++) {					// for each bank of filters
			filterFreqsArray[i] = new double[filterC];						
			prevX1[i] = new double[filterC];	prevX2[i] = new double[filterC];
			prevY1[i] = new double[filterC];	prevY2[i] = new double[filterC];
			V[i] = new double[filterC];  		K[i] = new double[filterC];  		norm[i] = new double[filterC];
			a0[i] = new double[filterC];		a1[i] = new double[filterC];		a2[i] = new double[filterC];
			b1[i] = new double[filterC];		b2[i] = new double[filterC];
			y[i] = new double[filterC];
		}

	}

	private void initTempArrays(int bankC, int filterC) {

		filterFreqsArrayTemp = new double[bankC][];
		QTemp = new double[filterC];
		QTemp = Q;

		for (int i=0; i<bankC; i++) {
			filterFreqsArrayTemp[i] = new double[filterC];
			filterFreqsArrayTemp[i] = filterFreqsArray[i];
		}

	}

	public void getAllFreqs() {
		for (int i=0; i<bankCount; i++) {
			System.out.println("Frequencies for bank "+i+" :");
			for (int j=0; j<filterCount; j++) {
				System.out.println("bank "+i+", freq "+j+":  "+filterFreqsArray[i][j] + "   Q: "+Q[j]);
			}
		}
	}
}
