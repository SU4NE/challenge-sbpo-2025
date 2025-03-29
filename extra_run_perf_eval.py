import os
import pandas as pd
import argparse
import re
from datetime import datetime
import matplotlib.pyplot as plt
from pathlib import Path
import seaborn as sns
from adjustText import adjust_text

timestamp_pattern = re.compile(r'(\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2})')

def process_files(input_dir: str):
    """
    Process all .txt files in the given directory and subdirectories, and generate a DataFrame with the following columns:

    - Instance: The name of the instance, extracted from the path of the .txt file.
    - File: The name of the .txt file, without the path.
    - Timestamp: The timestamp of the run, as extracted from the filename.
    - Is Solution Feasible: Whether the solution is feasible or not.
    - Objective Function Value: The value of the objective function.
    - Execution Time: The execution time in seconds.

    The DataFrame is sorted by the timestamp.

    Parameters
    ----------
    input_dir : str
        The path to the directory that contains the .txt files.

    Returns
    -------
    pandas.DataFrame
        The resulting DataFrame.
    """
    data = []

    for filename in Path.rglob(Path(input_dir), "*.txt"):
        file_path = filename
        match = timestamp_pattern.search(filename.stem)
        timestamp = match.group(1) if match else "Unknown"

        with open(file_path, 'r') as file:
            content = file.readlines()

            instance_name = None
            is_feasible = False
            objective_value = 0
            execution_time = 0

            for line in content:
                line = line.strip()

                if line.startswith("./"):
                    instance_name = line.split("/")[-1]

                elif "Is solution feasible:" in line:
                    is_feasible = line.split(":")[1].strip()

                elif "Objective function value:" in line:
                    objective_value = float(line.split(":")[1].strip())
                    
                elif "Execution time:" in line:
                    execution_time = float(line.split(":")[1].strip().split(" ")[0])

                    if instance_name:
                        data.append({
                            "Instance": instance_name,
                            "File": str(filename.stem),
                            "Timestamp": timestamp,
                            "Is Solution Feasible": is_feasible,
                            "Objective Function Value": objective_value,
                            "Execution Time": execution_time
                        })

                        instance_name = None
                        is_feasible = False
                        objective_value = 0
                        execution_time = 0

    df = pd.DataFrame(data)
    df["Timestamp"] = pd.to_datetime(df["Timestamp"], format="%Y-%m-%d_%H-%M-%S", errors='coerce')
    df = df.sort_values(by="Timestamp")

    return df

def generate_objective_function_plots(dataframe: pd.DataFrame, output_directory: str):
    """
    Generate and save various plots related to the objective function values over time for different instances.

    This function creates the following plots:
    1. Line plot showing the evolution of the objective function value over time for each instance, with percentage change annotations.
    2. Histogram displaying the distribution of objective function values.
    3. Line plot illustrating the relative change of the objective function value over time for each instance.
    4. Scatter plot showing the relationship between execution time and objective function value for different instances.

    Parameters
    ----------
    dataframe : pandas.DataFrame
        DataFrame containing the performance data with columns: 'Instance', 'Timestamp', 'Objective Function Value',
        'Execution Time', and 'Relative Change'.
    output_directory : str
        The directory where the generated plots will be saved.
    """
    # Plot: Evolution of Objective Function Value per Instance
    fig, ax = plt.subplots(figsize=(14, 6))
    sns.lineplot(data=dataframe, x="Timestamp", y="Objective Function Value", hue="Instance", marker="o", ax=ax)
    ax.set_xticklabels(ax.get_xticklabels(), rotation=45)
    ax.set_xlabel("Time")
    ax.set_ylabel("Objective Function Value")
    ax.set_title("Evolution of Objective Function Value per Instance")
    ax.legend(title="Instance", bbox_to_anchor=(1.05, 1), loc="upper left")

    # Add annotations with percentage change and arrow (blue if increase, red if decrease) for each instance
    texts = []
    for instance, group in dataframe.groupby("Instance"):
        group_sorted = group.sort_values("Timestamp")
        previous_value = None
        for idx, row in group_sorted.iterrows():
            if previous_value is not None and previous_value != 0:
                # Calculate the percentage change
                percent_change = ((row["Objective Function Value"] - previous_value) / previous_value) * 100
                # Define arrow and color: blue arrow (↑) if increase, red arrow (↓) if decrease
                arrow = "↑" if percent_change >= 0 else "↓"
                arrow_color = "blue" if percent_change >= 0 else "red"
                label = f"{abs(percent_change):.1f}% {arrow}"
                # Add the annotation and store the text object
                text_obj = ax.text(row["Timestamp"], row["Objective Function Value"], label,
                                   fontsize=9, ha="center", va="bottom", color=arrow_color)
                texts.append(text_obj)
            previous_value = row["Objective Function Value"]

    # Adjust the text annotations to avoid overlap
    adjust_text(texts, arrowprops=dict(arrowstyle="->", color='gray', lw=0.5))

    plt.tight_layout()
    plt.savefig(os.path.join(output_directory, "objective_evolution.png"))
    plt.close()

    # Histogram: Distribution of Objective Function Values
    fig, ax = plt.subplots(figsize=(12, 6))
    sns.histplot(dataframe["Objective Function Value"], bins=20, kde=True, color="blue", ax=ax)
    ax.set_xlabel("Objective Function Value")
    ax.set_ylabel("Frequency")
    ax.set_title("Distribution of Objective Function Values")
    plt.tight_layout()
    plt.savefig(os.path.join(output_directory, "objective_distribution.png"))
    plt.close()

    # Plot: Relative Change of Objective Function Value
    dataframe["Relative Change"] = dataframe.groupby("Instance")["Objective Function Value"].pct_change()
    fig, ax = plt.subplots(figsize=(14, 6))
    sns.lineplot(data=dataframe, x="Timestamp", y="Relative Change", hue="Instance", marker="o", ax=ax)
    ax.axhline(0, linestyle="--", color="gray")
    ax.set_xticklabels(ax.get_xticklabels(), rotation=45)
    ax.set_xlabel("Time")
    ax.set_ylabel("Relative Change")
    ax.set_title("Relative Change of Objective Function Value")
    ax.legend(title="Instance", bbox_to_anchor=(1.05, 1), loc="upper left")
    plt.tight_layout()
    plt.savefig(os.path.join(output_directory, "relative_change.png"))
    plt.close()

    # Plot: Relationship between Execution Time and Objective Function Value
    fig, ax = plt.subplots(figsize=(12, 6))
    sns.scatterplot(data=dataframe, x="Execution Time", y="Objective Function Value", hue="Instance", alpha=0.7, ax=ax)
    ax.set_xlabel("Execution Time (s)")
    ax.set_ylabel("Objective Function Value")
    ax.set_title("Relationship between Execution Time and Objective Function Value")
    ax.legend(title="Instance", bbox_to_anchor=(1.05, 1), loc="upper left")
    plt.tight_layout()
    plt.savefig(os.path.join(output_directory, "execution_vs_objective.png"))
    plt.close()

def generate_execution_time_variation_plot(dataframe: pd.DataFrame, output_directory: str) -> None:
    """Generate a plot illustrating the execution time variation for different instances.

    This function creates a line plot that visualizes the evolution of execution time for the top
    instances with the largest absolute change in execution time. Each line represents an instance,
    with annotations indicating the percentage change in execution time relative to the first recorded
    execution time.

    Parameters
    ----------
    dataframe : pandas.DataFrame
        DataFrame containing performance data with columns: 'Instance', 'File', 'Timestamp', and
        'Execution Time'.
    output_directory : str
        The directory where the generated plot will be saved.

    The plot is saved as 'execution_time_variation.png' in the specified output directory.
    """
    first_and_last_execution_times = dataframe.groupby("Instance")["Execution Time"].agg(["first", "last"])
    first_and_last_execution_times["Change"] = first_and_last_execution_times["last"] - first_and_last_execution_times["first"]
    first_and_last_execution_times["Absolute Change"] = first_and_last_execution_times["Change"].abs()

    top_instances = first_and_last_execution_times.nlargest(7, "Absolute Change").index.tolist()
    filtered_data = dataframe[dataframe["Instance"].isin(top_instances)]

    sorted_data = filtered_data.sort_values(["Instance", "Timestamp"])

    fig, ax = plt.subplots(figsize=(16, 8))

    annotations = []

    for instance, instance_data in sorted_data.groupby("Instance"):
        instance_data = instance_data.sort_values("Timestamp")
        first_execution_time = instance_data.iloc[0]["Execution Time"]

        sns.lineplot(data=instance_data, x="File", y="Execution Time", label=instance, linewidth=2, alpha=0.7, marker="o", markersize=7, ax=ax)

        for i in range(1, len(instance_data)):
            current_execution_time = instance_data.iloc[i]["Execution Time"]
            percentage_change = ((current_execution_time - first_execution_time) / first_execution_time) * 100

            annotation = ax.text(instance_data.iloc[i]["File"], current_execution_time, f"{percentage_change:.1f}%",
                                 fontsize=9, ha="center", va="bottom", color="black")
            annotations.append(annotation)

    adjust_text(annotations, arrowprops=dict(arrowstyle="-", color="gray", lw=0.5))

    ax.set_xticklabels(ax.get_xticklabels(), rotation=45)
    ax.set_xlabel("Arquivos")
    ax.set_ylabel("Tempo de Execução (s)")
    ax.set_title("Evolução do Tempo de Execução")
    ax.legend(title="Instância", bbox_to_anchor=(1.05, 1), loc="upper left")
    plt.tight_layout()
    plt.savefig(os.path.join(output_directory, "execution_time_variation.png"))
    plt.close()

def main():
    """
    Entry point of the script.

    Process .txt files in the given input directory, perform performance analysis, and save results to a CSV file.
    If the --plot option is specified, generate performance plots and save them to the output directory.

    Parameters
    ----------
    input_dir : str
        Directory path containing .txt files to be processed.
    output_dir : str
        Directory path to save results and plots.
    --plot : bool, optional
        If specified, generate performance plots and save them to the output directory.
    """
    parser = argparse.ArgumentParser(description="Process .txt files and perform performance analysis.")
    parser.add_argument("input_dir", type=str, help="Directory path containing .txt files")
    parser.add_argument("output_dir", type=str, help="Directory path to save results and plots")
    parser.add_argument("--plot", type=bool, help="Generate performance plots if specified")

    args = parser.parse_args()

    current_timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    results_dir = os.path.join(args.output_dir, current_timestamp)
    os.makedirs(results_dir, exist_ok=True)

    performance_df = process_files(args.input_dir)

    csv_path = os.path.join(results_dir, "results.csv")
    performance_df.to_csv(csv_path, index=False)

    if args.plot:
        generate_objective_function_plots(performance_df, results_dir)
        generate_execution_time_variation_plot(performance_df, results_dir)

if __name__ == "__main__":
    main()
