import sys
import colour
import matplotlib.pyplot as plt
from colour.plotting import plot_sds_in_chromaticity_diagram_CIE1931, plot_chromaticity_diagram_CIE1931
import numpy as np


def draw_plot(xs, ys, dxs, dys, path):
    plot_chromaticity_diagram_CIE1931(standalone=False)

    for i in range(len(xs)):
        x, y = xs[i], ys[i]
        xy = (x, y)
        plt.plot(x, y, 'o-', color='white')
        patch_name = "original-" + str(i + 1)
        plt.annotate(patch_name,
                     xy=xy,
                     xytext=(-50, 30),
                     textcoords='offset points',
                     arrowprops=dict(arrowstyle='->', connectionstyle='arc3, rad=-0.2'))


        x, y = xs[i] + dxs[i], ys[i] + dys[i]
        xy = (x, y)
        plt.plot(x, y, 'o-', color='black')
        patch_name = "measured-" + str(i + 1)
        plt.annotate(patch_name,
                     xy=xy,
                     xytext=(-50, 30),
                     textcoords='offset points',
                     arrowprops=dict(arrowstyle='->', connectionstyle='arc3, rad=-0.2'))

    fig = plt.gcf()
    fig.set_size_inches(5, 5)
    fig.savefig(path + "\\plot.png", dpi=96)


def main():
    test_count = (len(sys.argv) - 2) // 4
    path = sys.argv[1]
    x, y, dx, dy = [], [], [], []
    for i in range(test_count):
        x.append(float(sys.argv[2 * i + 2]))
        y.append(float(sys.argv[2 * i + 3]))
        dx.append(float(sys.argv[test_count * 2 + 2 * i + 2]))
        dy.append(float(sys.argv[test_count * 2 + 2 * i + 3]))

    draw_plot(x, y, dx, dy, path)


if __name__ == "__main__":
    main()
