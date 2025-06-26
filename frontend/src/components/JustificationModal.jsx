import React from 'react';

const JustificationModal = ({ open, justification, setJustification, onCancel, onConfirm }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 bg-white bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
          <h3 className="text-xl font-semibold text-white flex items-center gap-2">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Justification du recomptage
          </h3>
        </div>
        <div className="p-6">
          <p className="text-gray-600 mb-4">
            Veuillez expliquer les raisons nécessitant un recomptage pour ce produit.
          </p>
          <textarea
            value={justification}
            onChange={(e) => setJustification(e.target.value)}
            className="w-full h-32 p-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            placeholder="Décrivez les raisons du recomptage..."
          />
        </div>
        <div className="flex justify-end gap-3 p-6 bg-gray-50">
          <button onClick={onCancel} className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition">Annuler</button>
          <button onClick={onConfirm} className="px-5 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-md transition">Confirmer la demande</button>
        </div>
      </div>
    </div>
  );
};

export default JustificationModal; 