import numpy as np
import os
import matplotlib.pyplot as plt
from matplotlib.gridspec import GridSpec
from matplotlib.widgets import RadioButtons, TextBox, CheckButtons

# 3D Viewer using random data

def launch_3d_viewer():
    n_inline, n_crossline, n_time = 100, 100, 250
    cube = np.random.randn(n_inline, n_crossline, n_time).astype(np.float32)
    rms = np.sqrt(np.mean(cube ** 2))
    vmin, vmax = -rms, rms
    inline_idx = n_inline // 2
    crossline_idx = n_crossline // 2
    time_idx = n_time // 2

    fig = plt.figure(figsize=(12, 9))
    gs = GridSpec(2, 2, figure=fig, width_ratios=[1, 1], height_ratios=[1, 1], wspace=0.0, hspace=0.0)

    ax_inline_time = fig.add_subplot(gs[1, 0])
    ax_inline_xline = fig.add_subplot(gs[0, 0], sharex=ax_inline_time)
    ax_crossline_time = fig.add_subplot(gs[1, 1], sharey=ax_inline_time)
    ax_controls = fig.add_axes([0.81, 0.1, 0.18, 0.8])
    ax_controls.axis('off')

    current_cmap = 'gray'
    interp = ['nearest']

    img_ix = ax_inline_xline.imshow(cube[:, :, time_idx], cmap=current_cmap, vmin=vmin, vmax=vmax, origin='lower', interpolation=interp[0])
    img_it = ax_inline_time.imshow(cube[:, crossline_idx, :].T, cmap=current_cmap, vmin=vmin, vmax=vmax, origin='upper', aspect='auto', interpolation=interp[0])
    img_xt = ax_crossline_time.imshow(cube[inline_idx, :, :].T, cmap=current_cmap, vmin=vmin, vmax=vmax, origin='upper', aspect='auto', interpolation=interp[0])

    for ax in [ax_inline_xline, ax_crossline_time, ax_inline_time]:
        ax.label_outer()

    ax_inline_xline.set_ylabel('XLINE')
    ax_inline_time.set_xlabel('ILINE')
    ax_inline_time.set_ylabel('TIME')
    ax_crossline_time.set_xlabel('XLINE')

    crosshairs = {
        'ix_h': ax_inline_xline.axhline(crossline_idx, color='blue', lw=0.5),
        'ix_v': ax_inline_xline.axvline(inline_idx, color='blue', lw=0.5),
        'it_h': ax_inline_time.axhline(time_idx, color='blue', lw=0.5),
        'it_v': ax_inline_time.axvline(inline_idx, color='blue', lw=0.5),
        'xt_h': ax_crossline_time.axhline(time_idx, color='blue', lw=0.5),
        'xt_v': ax_crossline_time.axvline(crossline_idx, color='blue', lw=0.5)
    }

    annotation = fig.text(0.1, 0.95, '', fontsize=10)

    def update_display(i, j, k):
        img_ix.set_data(cube[:, :, k])
        img_it.set_data(cube[:, j, :].T)
        img_xt.set_data(cube[i, :, :].T)

        crosshairs['ix_h'].set_ydata([j, j])
        crosshairs['ix_v'].set_xdata([i, i])
        crosshairs['it_h'].set_ydata([k, k])
        crosshairs['it_v'].set_xdata([i, i])
        crosshairs['xt_h'].set_ydata([k, k])
        crosshairs['xt_v'].set_xdata([j, j])

        annotation.set_text(f"Inline: {i}, Crossline: {j}, Time: {k}, Value: {cube[i, j, k]:.3f}")
        fig.canvas.draw_idle()

    def apply_visual_settings():
        for img in [img_ix, img_it, img_xt]:
            img.set_cmap(current_cmap)
            img.set_clim(vmin, vmax)
            img.set_interpolation(interp[0])
        fig.canvas.draw_idle()

    def on_click(event):
        if event.inaxes in [ax_inline_xline, ax_inline_time, ax_crossline_time]:
            if event.inaxes == ax_inline_xline:
                i, j = int(event.xdata), int(event.ydata)
                update_display(i, j, time_idx)
            elif event.inaxes == ax_inline_time:
                i, k = int(event.xdata), int(event.ydata)
                update_display(i, crossline_idx, k)
            elif event.inaxes == ax_crossline_time:
                j, k = int(event.xdata), int(event.ydata)
                update_display(inline_idx, j, k)

    def on_move(event):
        if event.inaxes in [ax_inline_xline, ax_inline_time, ax_crossline_time] and event.xdata and event.ydata:
            if event.inaxes == ax_inline_xline:
                i, j, k = int(inline_idx), int(event.ydata), int(time_idx)
            elif event.inaxes == ax_inline_time:
                i, j, k = int(event.xdata), int(crossline_idx), int(event.ydata)
            elif event.inaxes == ax_crossline_time:
                i, j, k = int(inline_idx), int(event.xdata), int(event.ydata)
            if 0 <= i < n_inline and 0 <= j < n_crossline and 0 <= k < n_time:
                annotation.set_text(f"Inline: {i}, Crossline: {j}, Time: {k}, Value: {cube[i, j, k]:.3f}")
                fig.canvas.draw_idle()

    # Controls
    fig.text(0.82, 0.88, "Color Scale:", fontsize=10)
    radio_ax = fig.add_axes([0.82, 0.75, 0.15, 0.12])
    cmap_selector = RadioButtons(radio_ax, ('gray', 'seismic', 'viridis', 'hot'), active=0)

    def update_cmap(label):
        nonlocal current_cmap
        current_cmap = label
        apply_visual_settings()

    cmap_selector.on_clicked(update_cmap)

    fig.text(0.82, 0.68, "Scale Range:", fontsize=10)
    textbox_ax = fig.add_axes([0.82, 0.64, 0.15, 0.04])
    textbox = TextBox(textbox_ax, '', initial=f"{-rms:.2f},{rms:.2f}")

    def update_scale(text):
        nonlocal vmin, vmax
        try:
            parts = text.strip().split(',')
            if len(parts) == 2:
                vmin, vmax = float(parts[0]), float(parts[1])
                apply_visual_settings()
        except ValueError:
            pass  # ignore invalid input

    textbox.on_submit(update_scale)

    fig.text(0.82, 0.58, "Interpolation:", fontsize=10)
    check_ax = fig.add_axes([0.82, 0.54, 0.15, 0.04])
    check = CheckButtons(check_ax, ['Bilinear'], [False])

    def toggle_interp(label):
        interp[0] = 'bilinear' if check.get_status()[0] else 'nearest'
        apply_visual_settings()

    check.on_clicked(toggle_interp)

    fig.canvas.mpl_connect('button_press_event', on_click)
    fig.canvas.mpl_connect('motion_notify_event', on_move)

    update_display(inline_idx, crossline_idx, time_idx)
    plt.show()

if __name__ == "__main__":
    launch_3d_viewer()
