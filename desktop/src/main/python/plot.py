import sys
import colour
import matplotlib.pyplot as plt
from colour.plotting import plot_sds_in_chromaticity_diagram_CIE1931, plot_chromaticity_diagram_CIE1931
import numpy as np
from matplotlib.patches import FancyArrow, FancyArrowPatch


def draw_plot(xs, ys, dxs, dys, path):
    plot_chromaticity_diagram_CIE1931(standalone=False)

    arrows = []
    for i in range(len(xs)):
        x, y = xs[i], ys[i]
        xy = (x, y)
        plt.plot(x, y, 'o-', color='white')
        # patch_name = "original-" + str(i + 1)
        # plt.annotate(patch_name,
        #              xy=xy,
        #              xytext=(-50, 30),
        #              textcoords='offset points',
        #              arrowprops=dict(arrowstyle='->', connectionstyle='arc3, rad=-0.2'))


        x, y = xs[i] + dxs[i], ys[i] + dys[i]
        xy = (x, y)
        plt.plot(x, y, 'o-', color='black')
        # patch_name = "measured-" + str(i + 1)
        # plt.annotate(patch_name,
        #              xy=xy,
        #              xytext=(-50, 30),
        #              textcoords='offset points',
        #              arrowprops=dict(arrowstyle='->', connectionstyle='arc3, rad=-0.2'))

        plt.annotate("", xy=(xs[i] + dxs[i], ys[i] + dys[i]), xytext=(xs[i], ys[i]), arrowprops=dict(facecolor='black', arrowstyle='->'))

    plt.title("")
    fig = plt.gcf()
    fig.suptitle("")
    for arrow in arrows:
        fig.add_artist(arrow)

    # for i in range(4, 11, 2):
    fig.set_size_inches(10, 10)
    fig.savefig(path + "\\plot" + str(10) + ".png", dpi=96)


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
