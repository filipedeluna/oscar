import argparse

# Create parser for reading program arguments
import os
import shutil
import subprocess
import jellyfish as jf
import numpy
import numpy as np
import time
from tqdm import tqdm


# Convert a string to unicode
def to_unicode(code: int):
    return chr(int(str(code).zfill(8), 16))


def flatten_results_map(results_map):
    total = ""
    for key in results_map.keys():
        total += f"({key},{round(results_map[key], 4)})"
    return total


argparser = argparse.ArgumentParser(
    prog='testscript',
    description='Automate OSCAR\'s noise injection routine.',
    epilog='Run with argument -h for help.'
)

DISTANCE_ALGS = {
    0: "Levenshtein",
    1: "Damerau-Levenshtein",
    2: "Jaro",
    3: "Jaro-Wrinkler",
    4: "Hamming",
}

argparser.add_argument('program_dir', help='location of program to run.')
argparser.add_argument('executable', help='Name of program\'s main class to run or jar file name.')
argparser.add_argument('program_args', type=str, help='Arguments to be passed to program.')

argparser.add_argument('-c', '--count', default="30", type=str,
                       help='Number of times to run program (comma separated).')
argparser.add_argument('-da', '--distance_algorithm', default="0", type=int,
                       help=f'Distance algorithm: {DISTANCE_ALGS}.')
argparser.add_argument('-j', '--jar', action='store_true', help='Run program as a jar.')
argparser.add_argument('-dt', '--disable_thread_ids', action='store_true', help='Disable thread ID parsing.')
argparser.add_argument('-utl', '--unique_trace_locations', action='store_true',
                       help='Enable unique ids for repeated trace locations.')
argparser.add_argument('-uti', '--unordered_thread_ids', action='store_true', help='Maintain original thread ID order.')
argparser.add_argument('-dc', '--disable_coverage', action='store_true', help='Disable coverage analysis.')
argparser.add_argument('-di', '--disable_interleaving', action='store_true', help='Disable interleaving analysis.')
argparser.add_argument('-of', '--output_flags', type=str, help='Flags which will be checked in program output.')

argv = argparser.parse_args()

# Check distance alg valid
if argv.distance_algorithm < 0 or argv.distance_algorithm > len(DISTANCE_ALGS) - 1:
    print(f'Invalid distance algorithm.')
    exit(1)

# Check if file exists
if not os.path.isdir(argv.program_dir):
    print(f'Folder {argv.program_dir} not found')
    exit(1)

os.chdir(argv.program_dir)

# Remove old generated files
if os.path.isdir('oscar_output'):
    shutil.rmtree('oscar_output')

###############################################################################################################

# Save runtimes
runtimes = []

run_counts = []
for rc in str(argv.count).split(","):
    run_counts.append(int(rc))

runs = run_counts[len(run_counts) - 1]

print(f'Running program {argv.count} times')

FLAGS = str(argv.output_flags).split(",")
flags_detected = {}

# Run program x times
for i in tqdm(range(0, runs), desc="Variable Args"):
    start_time = time.time_ns() / 1_000_000

    if not argv.jar:
        result = subprocess.run(
            f'java {argv.executable} {argv.program_args}',
            shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE
        )
    else:
        result = subprocess.run(
            f'java -jar {argv.executable} {argv.program_args}',
            shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE
        )

    if result.returncode != 0:
        print(result.stderr.decode('utf-8'))
        print(result.stdout.decode('utf-8'))

    # Check the output for flags
    output = result.stdout.decode('utf-8')

    # Parse line by line to check for flags
    for line in output.split("\n"):
        for flag in FLAGS:
            if flag in line:
                if line in flags_detected:
                    flags_detected[line] = flags_detected[line] + 1
                else:
                    flags_detected[line] = 1
                break

    for rc in str(argv.count).split(","):
        run_counts.append(int(rc))

    runtimes.append(time.time_ns() / 1_000_000 - start_time)

print(f'Finished running. Analyzing files.')

###############################################################################################################


###############################################################################################################

print()
print("Results:")
print(f'\tAverage runtime (ms): {round(np.average(runtimes), 0)}')

for flag in flags_detected:
    print(f'\tDetected flag_-_-{flag}-_-_{flags_detected[flag]}')

if not argv.disable_coverage:
    # Try to analyze created files
    os.chdir('oscar_output')
    files = os.listdir('.')

    location_ids = {}
    interleavings = []
    trace_pairs = {}

    for file in files:
        content = open(file, 'r')  # .read()
        thread_ids = []
        trace_pairs_count = {}

        interleaving = ''

        # Get all thread ids for ordering
        for line in content:
            thread_id = int(line.split(' ')[0].strip())
            if thread_id not in thread_ids:
                thread_ids.append(thread_id)

        # Check if thread ids should maintain order when mapped
        if argv.unordered_thread_ids:
            thread_ids = numpy.sort(thread_ids)

        # Map thread ids
        mapped_thread_ids = {}
        for i in range(0, len(thread_ids)):
            mapped_thread_ids[thread_ids[i]] = to_unicode(i)

        # Parse normally
        content = open(file, 'r')

        for line in content:
            thread_id = mapped_thread_ids[int(line.split(' ')[0].strip())]

            # Make the interleaving id value start from 0
            location_id = line.split(' ')[1].strip()
            if location_id not in location_ids:
                location_ids[location_id] = to_unicode(len(location_ids) + len(thread_ids))
            location_id = location_ids[location_id]

            # Append content with or without thread id
            trace_pair = location_id
            if not argv.disable_thread_ids:
                trace_pair = f'{thread_id}{trace_pair}'

            # Check if this interleaving pair is duplicate and needs new assigned id
            if argv.unique_trace_locations:
                if trace_pair not in trace_pairs_count:
                    trace_pairs_count[trace_pair] = 0
                trace_pairs_count[trace_pair] += 1

                trace_pair = f'{trace_pairs_count[trace_pair]}_{trace_pair}'

            # Transform interleaving pair representation in single mapped unicode
            if trace_pair not in trace_pairs:
                trace_pairs[trace_pair] = to_unicode(len(trace_pairs))

            interleaving += trace_pairs[trace_pair]

        interleavings.append(interleaving)

    # Parse interleavings
    avg_dist_runs = {}
    std_dev_runs = {}
    uniq_interleavings_runs = {}
    avg_cluster_size = {}

    for rc in run_counts:
        interleavings_split = interleavings[0:rc]
        uniq_interleavings_runs[rc] = len(set(interleavings_split))

        clusters = {}
        # Calculate avg cluster size
        for interleaving in interleavings_split:
            if interleaving not in clusters:
                clusters[interleaving] = 1
            else:
                clusters[interleaving] += 1

        avg_cluster_size[rc] = np.average(list(clusters.values()))

        # For regular pairs
        interleaving_dists = []
        interleaving_dist = 0

        # Calculate average ratio
        if not argv.disable_coverage:
            for x in range(0, len(interleavings_split) - 1):
                for y in range(x + 1, len(interleavings_split)):
                    ix = interleavings_split[x]
                    iy = interleavings_split[y]

                    # Levenshtein
                    if argv.distance_algorithm == 0:
                        interleaving_dist = jf.levenshtein_distance(ix, iy)

                    # Damerau-Levenshtein
                    if argv.distance_algorithm == 1:
                        interleaving_dist = jf.damerau_levenshtein_distance(ix, iy)

                    # Jaro
                    if argv.distance_algorithm == 2:
                        interleaving_dist = jf.jaro_similarity(ix, iy)

                    # Jaro-Wrinkler
                    if argv.distance_algorithm == 3:
                        interleaving_dist = jf.jaro_winkler_similarity(ix, iy)

                    # Hamming
                    if argv.distance_algorithm == 4:
                        interleaving_dist = jf.hamming_distance(ix, iy)

                    interleaving_dists.append(interleaving_dist)
        else:
            interleaving_dists.append(1)

        avg_dist_runs[rc] = round(np.average(interleaving_dists), 4)
        std_dev_runs[rc] = round(float(np.std(interleaving_dists)), 4)

    distance_alg = DISTANCE_ALGS[argv.distance_algorithm]
    print(f'\tUnique interleavings: {flatten_results_map(uniq_interleavings_runs)}')
    print(f'\tAverage {distance_alg} distance: {flatten_results_map(avg_dist_runs)}')
    print(f'\t{distance_alg} distance standard deviation: {flatten_results_map(std_dev_runs)}')
    print(f'\tAverage Cluster Size: {flatten_results_map(avg_cluster_size)}')
