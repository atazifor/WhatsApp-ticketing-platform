package com.nourri.busticketing.service;

import com.nourri.busticketing.LayoutAssets;
import com.nourri.busticketing.config.FontConfig;
import com.nourri.busticketing.generator.BusLayoutOverlay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ImageOverlayService {
    private final BusLayoutOverlay overlay;

    @Autowired
    public ImageOverlayService(LayoutAssets layoutAssets, FontConfig fontConfig) {
        this.overlay = new BusLayoutOverlay(layoutAssets, fontConfig.legendFont());
    }

    public String createAvailabilityOverlay(Set<String> availableSeats) {
        return overlay.createAvailabilityOverlay(availableSeats);
    }

    public String createImageWithHighlights(List<String> selectedSeats) {
        return overlay.createImageWithHighlights(selectedSeats);
    }
}
