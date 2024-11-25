import ij.*;
import ij.io.*;
import ij.process.*;
import ij.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class GRDM_U4 implements PlugInFilter {

	protected ImagePlus imp;
	final static String[] choices = { "Wischen", "Weiche Blende", "Overlay A über B", "Overlay B über A", "Schieben", "Chroma Key", "Extra" };

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_RGB + STACK_REQUIRED;
	}

	public static void main(String args[]) {
		ImageJ ij = new ImageJ(); // neue ImageJ Instanz starten und anzeigen
		ij.exitWhenQuitting(true);

		IJ.open("StackB.zip");

		GRDM_U4 sd = new GRDM_U4();
		sd.imp = IJ.getImage();
		ImageProcessor B_ip = sd.imp.getProcessor();
		sd.run(B_ip);
	}

	public void run(ImageProcessor B_ip) {
		// Film B wird uebergeben
		ImageStack stack_B = imp.getStack();

		int length = stack_B.getSize();
		int width = B_ip.getWidth();
		int height = B_ip.getHeight();

		// ermoeglicht das Laden eines Bildes / Films
		Opener o = new Opener();
//		OpenDialog od_A = new OpenDialog("Auswählen des 2. Filmes ...",  "");
//				
//		// Film A wird dazugeladen
//		String dateiA = od_A.getFileName();
//		if (dateiA == null) return; // Abbruch
//		String pfadA = od_A.getDirectory();
		ImagePlus A = o.openImage("StackA.zip");
		if (A == null)
			return; // Abbruch

		ImageProcessor A_ip = A.getProcessor();
		ImageStack stack_A = A.getStack();

		if (A_ip.getWidth() != width || A_ip.getHeight() != height) {
			IJ.showMessage("Fehler", "Bildgrößen passen nicht zusammen");
			return;
		}

		// Neuen Film (Stack) "Erg" mit der kleineren Laenge von beiden erzeugen
		length = Math.min(length, stack_A.getSize());

		ImagePlus Erg = NewImage.createRGBImage("Ergebnis", width, height, length, NewImage.FILL_BLACK);
		ImageStack stack_Erg = Erg.getStack();

		// Dialog fuer Auswahl des Ueberlagerungsmodus
		GenericDialog gd = new GenericDialog("Überlagerung");
		gd.addChoice("Methode", choices, "");
		gd.showDialog();

		int methode = 0;
		String s = gd.getNextChoice();
		if (s.equals("Wischen")) methode = 1;
		if (s.equals("Weiche Blende")) methode = 2;
		if (s.equals("Overlay A über B")) methode = 3;
		if (s.equals("Overlay B über A")) methode = 4;
		if (s.equals("Schieben")) methode = 5;
		if (s.equals("Chroma Key")) methode = 6;
		if (s.equals("Extra")) methode = 7;

		// Arrays fuer die einzelnen Bilder
		int[] pixels_B;
		int[] pixels_A;
		int[] pixels_Erg;

		// Schleife ueber alle Bilder
		for (int z = 1; z <= length; z++) {
			pixels_B = (int[]) stack_B.getPixels(z);
			pixels_A = (int[]) stack_A.getPixels(z);
			pixels_Erg = (int[]) stack_Erg.getPixels(z);

			int pos = 0;
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++, pos++) {
					int cA = pixels_A[pos];
					int rA = (cA & 0xff0000) >> 16;
					int gA = (cA & 0x00ff00) >> 8;
					int bA = (cA & 0x0000ff);

					int cB = pixels_B[pos];
					int rB = (cB & 0xff0000) >> 16;
					int gB = (cB & 0x00ff00) >> 8;
					int bB = (cB & 0x0000ff);

					// Wischen
					if (methode == 1) {
						if (y + 1 > (z - 1) * (double) width / (length - 1))
							pixels_Erg[pos] = pixels_B[pos];
						else
							pixels_Erg[pos] = pixels_A[pos];
					}

					// Weiche Blende
					if (methode == 2) {
						int r, g, b;						
						double mask = (1.0 * (z - 1) / (length - 1)) * 255;
						
						r = (int) ((rA * mask + rB * (1.0 * 255 - mask)) / 255);
						g = (int) ((gA * mask + gB * (1.0 * 255 - mask)) / 255);
						b = (int) ((bA * mask + bB * (1.0 * 255 - mask)) / 255);
						
						pixels_Erg[pos] = (r << 16) | (g << 8) | b;
					}

					// Overlay A über B
					if (methode == 3) {
						int r, g, b;
						if ((rB+gB+bB)/3 < 128) {
							r = (rA * rB) / 128;
							g = (gA * gB) / 128;
							b = (bA * bB) / 128;
						} else {
							r = 255 - ((255-rA) * (255-rB)/128);
							g = 255 - ((255-gA) * (255-gB)/128);
							b = 255 - ((255-bA) * (255-bB)/128);
						}
						pixels_Erg[pos] = (r << 16) | (g << 8) | b;
					}
					
					// Overlay B über A
					if (methode == 4) {
						int r, g, b;
						if (rA < 128) {
							r = (rA * rB) / 128;
						} else {
							r = 255 - ((255-rA) * (255-rB)/128);
						}
						if (gA < 128) {
							g = (gA * gB) / 128;
						} else {
							g = 255 - ((255-gA) * (255-gB)/128);
						}
						if (bA < 128) {
							b = (bA * bB) / 128;
						} else {
							b = 255 - ((255-bA) * (255-bB)/128);
						}
						pixels_Erg[pos] = (r << 16) | (g << 8) | b;
					}

					// Schieben
					if (methode == 5) {
						if (x + 1 > (z - 1) * (double) width / (length - 1)) {
							pixels_Erg[pos] = pixels_B[(int) (pos - (z - 1) * (double) width / (length - 1))];}
						else
							pixels_Erg[pos] = pixels_A[(int) (pos + width - (z - 1) * (double) width / (length - 1))];
					}

					// Chroma Key
					if (methode == 6) {
						double dist = Math.sqrt(Math.pow(255-rA, 2) + Math.pow(88-gA, 2) + Math.pow(32-bA, 2));
						
						if (dist < 128) {
							pixels_Erg[pos] = pixels_B[pos];
						} else {
							pixels_Erg[pos] = pixels_A[pos];
						}
					}

					// Eigener Übergang, Kombi aus Wischen und Streifen der USA-Flagge ;)
					if (methode == 7) {
						double stripe = height / 10.0;
						
						if (1.0 * y % (2*stripe) >= stripe ) {
							if (x + 1 > (z - 1) * (double) width / (length - 1))
								pixels_Erg[pos] = pixels_B[pos];
							else
								pixels_Erg[pos] = pixels_A[pos];
						} else {
							if (width - x + 1 < (z - 1) * (double) width / (length - 1))
								pixels_Erg[pos] = pixels_A[pos];
							else
								pixels_Erg[pos] = pixels_B[pos];
						}
					}

				}
		}

		// neues Bild anzeigen
		Erg.show();
		Erg.updateAndDraw();

	}

}
