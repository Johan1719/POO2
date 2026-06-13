package com.laptitefrance.delivery.views;

import javax.swing.*;
import java.awt.*;

/**
 * Paginador reutilizable (Anterior/Siguiente + label de estado).
 */
public class PaginatorPanel extends JPanel {

    public interface PageChangeListener {
        void onPageChange(int newPage);
    }

    private final JButton btnPrev;
    private final JButton btnNext;
    private final JLabel lbl;

    private int page;
    private int totalPages;
    private final PageChangeListener listener;

    public PaginatorPanel(int initialPage, int initialTotalPages, PageChangeListener listener) {
        this.listener = listener;
        this.page = Math.max(1, initialPage);
        this.totalPages = Math.max(1, initialTotalPages);

        setLayout(new FlowLayout(FlowLayout.CENTER));
        btnPrev = new JButton("<< Anterior");
        btnNext = new JButton("Siguiente >>");
        lbl = new JLabel();

        btnPrev.addActionListener(e -> {
            if (page > 1) {
                setPage(page - 1);
                listener.onPageChange(this.page);
            }
        });

        btnNext.addActionListener(e -> {
            if (page < totalPages) {
                setPage(page + 1);
                listener.onPageChange(this.page);
            }
        });

        add(btnPrev);
        add(lbl);
        add(btnNext);

        refresh();
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = Math.max(1, totalPages);
        if (page > this.totalPages) {
            page = this.totalPages;
        }
        refresh();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
        if (this.page > totalPages) {
            this.page = totalPages;
        }
        refresh();
    }

    private void refresh() {
        lbl.setText(String.format("Página %d de %d", page, totalPages));
        btnPrev.setEnabled(page > 1);
        btnNext.setEnabled(page < totalPages);
    }
}

