
import numpy as np



if __name__ == '__main__':
    source_xyz = np.array([[0.2, 0.1, 0.1],
                  [0.1, 0.2, 0.2],
                  [0.1, 0.1, 0.2]])
    reference_xyz = np.array([[1., 0., 0.],
                  [0., 2., 2.],
                  [0., 0., 1.]])

    source_xyz_hm = np.append(source_xyz, np.ones((3, 1)), axis=1)
    ccm = np.linalg.pinv(source_xyz_hm).dot(reference_xyz)
    print(ccm)