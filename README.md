## Automatic Receipt Scanner Application

##### Automatically determines the receipt or piece of paper in frame.
- Reads the current frame in and converts it to greyscale.
- Applies a bilaterial filter blur to remove noise on the greyscale image and maintain the edges
	- This is a performance impact but will result in better scanning results than a gaussian blur.
	- Diameter is set to 5 (diameter of each pixel neighborhood)
	- Sigma color is set to 200 (the larger this number, the more colors that are mixed together)
	- Sigma space is set to 200 (the larger this number, the more impact farther pixels will have on each other)
- Using the blurred, greyscale image apply canny edge detection with a low hysteresis of 0 and a high hysteresis of 50.
	- Low hysteresis and high hysteresis are set to fairly low values to keep most of the edges (in case of poor gradients)
- Using the resulting edges, determine the contours that are present
	- The contours are then sorted with the largest contour being used as the presumed document
		- The contour must have at least 4 points to reduce false positives
		- The contour must be at least 50% of the height and width of the overall image to reduce false positives
- This frame is kept and then compared against the next frame found. If the contour's point are in the same position (with a 10% tolerance on those positions, it will be kept). This needs to happen 5 times in a row for the frame to be saved since it is the likely document.
- Once the contour is found 5 times in a row, the image is cropped to just be the document found in frame and saved to storage as a RGB image.
- The cropped RGB image is converted to greyscale and then an inverse binary mask is applied to it with a range of 100-255 so that greyscale values falling in that range are turned to white and values not in it are kept as black. The assumption for this to work is that the text is black in the image, if it is not then this will not work.
- In the cropped image, the contours are then found (which is the text and images in the document). These contours are filtered to remove false positives (if the contours are too small or too large). The contours are then further reduced by combining the bounding boxes on the contours if the horizontal difference between them are similar (ie. if they are on the same line then they become one). This is to make sure that the price and the name of the product are kept together.
- The resulting text contours are fed to the OCR library Tesseract. The text that is found is kept only if a dollar sign and period are found in the resulting text.
