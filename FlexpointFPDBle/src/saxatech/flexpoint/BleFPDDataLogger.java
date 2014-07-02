package saxatech.flexpoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import android.os.Environment;

public class BleFPDDataLogger {
	public static final int DEVICE_TYPE_LEFT_SHOE  = BleFPDDeviceGroup.DEVICE_TYPE_LEFT_SHOE;
	public static final int DEVICE_TYPE_RIGHT_SHOE = BleFPDDeviceGroup.DEVICE_TYPE_RIGHT_SHOE;
	public static final int DEVICE_TYPE_CLUB       = BleFPDDeviceGroup.DEVICE_TYPE_CLUB;
	
	private FileOutput aioOutput;
	
	public static BleFPDDataLogger createDataLogger(
		int insoleType
		) throws FileNotFoundException
	{
		return createDataLogger(
			insoleType, new java.util.Date()
			);
	}
	
	public static BleFPDDataLogger createDataLogger(
			int insoleType, java.util.Date date
			) throws FileNotFoundException
		{
			char T = 'U';
			if (insoleType == DEVICE_TYPE_LEFT_SHOE)
				T = 'L';
			else if (insoleType == DEVICE_TYPE_RIGHT_SHOE)
				T = 'R';
			else if (insoleType == DEVICE_TYPE_CLUB)
				T = 'C';
						
			final String dateTimeFileName =
				android.text.format.DateFormat.format(
						"_yyyy_MM_dd_kkmmss", date).toString();
			
			BleFPDDataLogger logger = new BleFPDDataLogger();
			
			try {
				File dir = getOutputDirectory();
				logger.aioOutput = FileOutput.open(
					dir, "FPD_" + T + dateTimeFileName + ".csv"
					);
				
			} catch (FileNotFoundException e) {
				logger.close();
				throw e;
			}
			
			return logger;
		}
	
	private BleFPDDataLogger() {
	}
	
	public synchronized void close() {		
		if (aioOutput != null) {
			aioOutput.close();
			aioOutput = null;
		}
	}
	
	public synchronized void writeData(
		long timeStamp,
		int fs0, int fs1, int fs2, int fs3, int fs4,
		int acX, int acY, int acZ,
		int mgX, int mgY, int mgZ
		)
	{
		if (aioOutput == null)
			return;
		
		aioOutput.write(
			String.format("%d,  %d,%d,%d,%d,%d, %d,%d,%d, %d,%d,%d",
				timeStamp,
				fs0, fs1, fs2, fs3, fs4,
				acX, acY, acZ,
				mgX, mgY, mgZ
				)
			);
		
	}
		
	private static final String SD_SUB_DIR = "/FlexpointData";
	
	private static File getOutputDirectory() {
		File sdcard = Environment.getExternalStorageDirectory();
		File dir = new File(sdcard.getAbsolutePath() + SD_SUB_DIR);
		dir.mkdir();
		return dir;
	}
	
	private static class FileOutput {
		PrintWriter printer;
		
		public static FileOutput open(
			File dir, String filename) throws FileNotFoundException
		{	
			FileOutput fo = new FileOutput();
			fo.printer = new PrintWriter(new File(dir, filename));
			return fo;
		}
		public void close() {
			printer.close();
		}
		public void write(String data) {
			printer.println(data);
		}
	}
}
