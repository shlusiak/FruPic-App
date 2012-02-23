package de.saschahlusiak.frupic.utils;

public interface ProgressTaskActivityInterface {
	void updateProgressDialog(int progress, int max);
	void success();
	void dismiss();
}
