import { useEffect, useState } from "react";
import { Doughnut } from "react-chartjs-2";
import { Box, Typography, CircularProgress } from "@mui/joy";
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
} from "chart.js";
import axios from "axios";

ChartJS.register(ArcElement, Tooltip, Legend);

const PlanStatusChart = () => {
  const [chartData, setChartData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isEmpty, setIsEmpty] = useState(false);

  const colors = ["#4caf50", "#2196f3", "#ff9800", "#f44336", "#9c27b0", "#00bcd4"];

  useEffect(() => {
    axios.get("http://localhost:8080/api/plans/countByStatus")
      .then((res) => {
        const data = res.data;
        const labels = Object.keys(data);
        const values = Object.values(data).map(Number);
        const allZero = values.every((v) => v === 0);
        setIsEmpty(allZero);

        const dataset = allZero
          ? {
              labels: ["Aucun plan"],
              datasets: [
                {
                  data: [1],
                  backgroundColor: ["#d1d1d1"],
                  borderWidth: 0,
                },
              ],
            }
          : {
              labels,
              datasets: [
                {
                  data: values,
                  backgroundColor: colors.slice(0, values.length),
                  borderWidth: 0,
                },
              ],
            };

        setChartData(dataset);
      })
      .catch((err) => {
        console.error("Erreur chargement des plans :", err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: "bottom",
        labels: {
          boxWidth: 10,
          font: {
            size: 11,
          },
        },
      },
    },
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" mt={2}>
        <CircularProgress size="sm" />
      </Box>
    );
  }

  return chartData ? (
    <Box
      sx={{
        borderRadius: 4,
        boxShadow: "sm",
        bgcolor: "#FFFFFF",
        p: 2,
        width: { xs: "100%", sm: 300, md: 320 },
        maxWidth: '100%',
        height: { xs: "auto", sm: 320 },
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
      }}
    >
      <Typography level="title-sm" fontWeight={600} mb={1}>
        Plans par statut
      </Typography>
      <Box sx={{ width: "100%", height: 220 }}>
        <Doughnut data={chartData} options={options} />
      </Box>
      {isEmpty && (
        <Typography level="body-xs" color="neutral" mt={1}>
          Aucun plan à afficher
        </Typography>
      )}
    </Box>
  ) : (
    <Typography color="danger" level="body-sm">
      Impossible de charger les données.
    </Typography>
  );
};

export default PlanStatusChart;
