import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { 
  Box, 
  Typography, 
  CircularProgress, 
  Alert, 
  LinearProgress 
} from '@mui/joy';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';

const VerifyAccount = () => {
  const navigate = useNavigate();
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const [progress, setProgress] = useState(0);
  const [isVerifying, setIsVerifying] = useState(true);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');

    if (!token) {
      setStatus('error');
      setMessage("Aucun token fourni.");
      return;
    }

    const verifyAccount = async () => {
      try {
        console.log("Starting verification with token:", token);
        // First, check if token exists and get user email
        const checkResponse = await axios.get(`http://localhost:8080/users/check-token?token=${token}`);
        const userEmail = checkResponse.data.email;

        // Then validate the token
        const response = await axios.get(`http://localhost:8080/users/validate?token=${token}`);
        console.log("Initial verification response:", response.data);
        
        setStatus('verifying');
        setMessage("Compte vérifié, vérification de l'activation...");

        // Poll for enabled status
        const checkEnabled = async () => {
          try {
            const userResponse = await axios.get(
              `http://localhost:8080/users/check-status?email=${userEmail}`
            );
            
            if (userResponse.data.enabled) {
              setStatus('success');
              setMessage("Compte vérifié et activé avec succès!");
              
              // Redirect countdown
              let secondsLeft = 5;
              const redirectInterval = setInterval(() => {
                secondsLeft--;
                setProgress((5 - secondsLeft) * 20);
                
                if (secondsLeft === 0) {
                  clearInterval(redirectInterval);
                  navigate('/login');
                }
              }, 1000);
            }
          } catch (error) {
            console.error("Status check error:", error);
            setStatus('error');
            setMessage("Erreur lors de la vérification du statut du compte.");
          }
        };

        // Wait 5 seconds before checking enabled status
        setTimeout(() => {
          checkEnabled();
          setIsVerifying(false);
        }, 5000);

      } catch (error) {
        console.error("Verification error:", error.response?.data);
        setStatus('error');
        setMessage(error.response?.data?.message || 'Erreur de validation du compte.');
        setIsVerifying(false);
      }
    };

    verifyAccount();
  }, [navigate]);

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      minHeight="100vh"
      bgcolor="background.default"
      p={3}
    >
      <Box
        bgcolor="white"
        borderRadius={8}
        p={4}
        boxShadow={3}
        maxWidth={400}
        width="100%"
      >
        {status === 'loading' && (
          <>
            <CircularProgress size="lg" sx={{ mb: 2 }} />
            <Typography level="h5" textAlign="center">
              Vérification en cours...
            </Typography>
          </>
        )}

        {status === 'verifying' && (
          <>
            <CircularProgress size="lg" sx={{ mb: 2 }} />
            <Typography level="h5" textAlign="center">
              {message}
            </Typography>
          </>
        )}

        {status === 'success' && (
          <>
            <Alert
              startDecorator={<CheckCircleIcon />}
              color="success"
              sx={{ mb: 2 }}
            >
              {message}
            </Alert>
            <Typography level="body-md" textAlign="center" mb={2}>
              Redirection vers la page de connexion dans {5 - Math.floor(progress/20)} secondes...
            </Typography>
            <LinearProgress 
              determinate 
              value={progress} 
              sx={{ mb: 2 }} 
              color="success"
            />
          </>
        )}

        {status === 'error' && (
          <Alert
            startDecorator={<ErrorIcon />}
            color="danger"
            sx={{ mb: 2 }}
          >
            {message}
          </Alert>
        )}
      </Box>
    </Box>
  );
};

export default VerifyAccount;
