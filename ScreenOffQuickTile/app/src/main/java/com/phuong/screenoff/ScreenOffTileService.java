package com.phuong.screenoff;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class ScreenOffTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel(getString(R.string.tile_label));
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!ScreenOffAccessibilityService.lockScreenNow()) {
            Toast.makeText(this, R.string.enable_accessibility_first, Toast.LENGTH_LONG).show();
        }
    }
}
