/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.standalone.ui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import es.gob.afirma.core.signers.AdESPolicy;

@SuppressWarnings("nls")
final class PreferencesPanel extends JPanel {

	private static final String PREFERENCE_SIGNATURE_ALGORITHM = "signatureAlgorithm"; //$NON-NLS-1$
	private static final String PREFERENCE_POLICY_IDENTIFIER = "policyIdentifier"; //$NON-NLS-1$
	private static final String PREFERENCE_POLICY_IDENTIFIER_HASH = "policyIdentifierHash"; //$NON-NLS-1$
	private static final String PREFERENCE_POLICY_IDENTIFIER_HASH_ALGORITHM = "policyIdentifierHashAlgorithm"; //$NON-NLS-1$
	private static final String PREFERENCE_POLICY_QUALIFIER = "policyQualifier"; //$NON-NLS-1$

	private static final long serialVersionUID = -3168095095548385291L;

	private static final Preferences PREFERENCES = Preferences.userRoot();

	private static final AdESPolicy POLICY_AGE = new AdESPolicy(
		"http://politica/id",
		"AAAA",
		"SHA1",
		"http://politica/qualifier"
	);

	private static final int POLICY_INDEX_AGE = 1;

	private static final int POLICY_INDEX_NONE = 0;

	private static final AdESPolicy POLICY_CUSTOM;
	static {
		AdESPolicy customPolicy = null;
		try {
			customPolicy = new AdESPolicy(
				PREFERENCES.get(PREFERENCE_POLICY_IDENTIFIER, null),
				PREFERENCES.get(PREFERENCE_POLICY_IDENTIFIER_HASH, null),
				PREFERENCES.get(PREFERENCE_POLICY_IDENTIFIER_HASH_ALGORITHM, null),
				PREFERENCES.get(PREFERENCE_POLICY_QUALIFIER, null)
			);
		}
		catch(final Exception e) {
			// Se ignora
		}
		POLICY_CUSTOM = customPolicy;
	}

	private static final List<AdESPolicy> preloadedPolicies = new ArrayList<AdESPolicy>(1);
	static {
		preloadedPolicies.add(
			POLICY_INDEX_NONE, // Por posicion, en el 0 ninguna politica
			null
		);
		preloadedPolicies.add(
			POLICY_INDEX_AGE, // Por posicion, en el 1 la politica de la AGE
			POLICY_AGE
		);
		preloadedPolicies.add(
			2, // Por posicion, en el ultimo lugar una politica a medida
			POLICY_CUSTOM
		);
	}
	static List<AdESPolicy> getPreloadedPolicies() {
		return preloadedPolicies;
	}

	private final JComboBox signarureAlgorithms = new JComboBox();

	private final JComboBox policies = new JComboBox();
	JComboBox getPolicies() {
		return this.policies;
	}

	private final JTextField policyIdentifier = new JTextField();
	JTextField getPolicyIdentifier() {
		return this.policyIdentifier;
	}

	private final JTextField policyIdentifierHash = new JTextField();
	JTextField getPolicyIdentifierHash() {
		return this.policyIdentifierHash;
	}

	private final JComboBox policyIdentifierHashAlgorithm = new JComboBox(new String[] {
		"SHA1", //$NON-NLS-1$
		"SHA-512", //$NON-NLS-1$
		"SHA-384", //$NON-NLS-1$
		"SHA-256" //$NON-NLS-1$
	});
	JComboBox getPolicyIdentifierHashAlgorithm() {
		return this.policyIdentifierHashAlgorithm;
	}

	private final JTextField policyQualifier =new JTextField();
	JTextField getPolicyQualifier() {
		return this.policyQualifier;
	}


	void createUI() {
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("General", createGeneralPanel());

		add(tabbedPane);
		add(createButtonsPanel());
	}

	void savePreferences() {
		if (this.policies.getSelectedIndex() != POLICY_INDEX_NONE) {
			try {
				// Construimos el objeto para ver si los datos son correctos, ya que se comprueban en el constructor
				new AdESPolicy(
					PreferencesPanel.this.getPolicyIdentifier().getText(),
					PreferencesPanel.this.getPolicyIdentifierHash().getText(),
					PreferencesPanel.this.getPolicyIdentifierHashAlgorithm().getSelectedItem().toString(),
					PreferencesPanel.this.getPolicyQualifier().getText()
				);
			}
			catch(final Exception e) {
				JOptionPane.showMessageDialog(
					this,
					"<html><p>" + "Los datos de la pol�tica de firma son inv�lidos" + ":<br>" + e.getLocalizedMessage() + "</p></html>", //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$
					"Error",
					JOptionPane.ERROR_MESSAGE
				);
				return;
			}
			PreferencesPanel.PREFERENCES.put(PREFERENCE_POLICY_IDENTIFIER, this.policyIdentifier.getText());
			PreferencesPanel.PREFERENCES.put(PREFERENCE_POLICY_IDENTIFIER_HASH, this.policyIdentifierHash.getText());
			PreferencesPanel.PREFERENCES.put(PREFERENCE_POLICY_IDENTIFIER_HASH_ALGORITHM, this.policyIdentifierHashAlgorithm.getSelectedItem().toString());
			PreferencesPanel.PREFERENCES.put(PREFERENCE_POLICY_QUALIFIER, this.policyQualifier.getText());
		}
		else {
			PreferencesPanel.PREFERENCES.remove(PREFERENCE_POLICY_IDENTIFIER);
			PreferencesPanel.PREFERENCES.remove(PREFERENCE_POLICY_IDENTIFIER_HASH);
			PreferencesPanel.PREFERENCES.remove(PREFERENCE_POLICY_IDENTIFIER_HASH_ALGORITHM);
			PreferencesPanel.PREFERENCES.remove(PREFERENCE_POLICY_QUALIFIER);
		}

		PreferencesPanel.PREFERENCES.put(PREFERENCE_SIGNATURE_ALGORITHM, this.signarureAlgorithms.getSelectedItem().toString());
	}

	private JPanel createGeneralPanel() {
		final JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Opciones de firma"));
		final JPanel signatureAgorithmPanel = new JPanel();
		signatureAgorithmPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Algoritmo de firma"));
		this.signarureAlgorithms.setModel(new DefaultComboBoxModel(new String[] {
			"SHA1withRSA", //$NON-NLS-1$
			"SHA512withRSA", //$NON-NLS-1$
			"SHA384withRSA", //$NON-NLS-1$
			"SHA256withRSA" //$NON-NLS-1$
		}));
		this.signarureAlgorithms.setSelectedItem(PreferencesPanel.PREFERENCES.get(PREFERENCE_SIGNATURE_ALGORITHM, "SHA1withRSA")); //$NON-NLS-1$
		signatureAgorithmPanel.add(this.signarureAlgorithms);
		panel.add(signatureAgorithmPanel);
		panel.add(createPolicyPanel());
		return panel;
	}

	private JPanel createPolicyPanel() {
		final JPanel panel = new JPanel();

		final GridLayout layout = new GridLayout(0,1);
		panel.setLayout(layout);

		panel.setBorder(BorderFactory.createTitledBorder("Pol�tica de firma"));

		// Los elementos del menu desplegable se identifican por su orden
		this.policies.setModel(new DefaultComboBoxModel(new String[] {
			"Ninguna pol�tica",				// Ninguna pol�tica, debe ser el primer elemento
			"Pol�tica de firma de la AGE",	// Politica de la AGE, debe ser el segundo elemento
			"Pol�tica a medida"				// Politica a medida, debe ser el �ltimo elemento
		}));
		panel.add(this.policies);
		this.policies.addItemListener(new ItemListener() {
			/** {@inheritDoc} */
			@Override
			public void itemStateChanged(final ItemEvent ie) {

				final boolean enabled = PreferencesPanel.this.getPolicies().getSelectedIndex() == (PreferencesPanel.this.getPolicies().getItemCount()-1);
				PreferencesPanel.this.getPolicyIdentifier().setEnabled(enabled);
				PreferencesPanel.this.getPolicyIdentifierHash().setEnabled(enabled);
				PreferencesPanel.this.getPolicyIdentifierHashAlgorithm().setEnabled(enabled);
				PreferencesPanel.this.getPolicyQualifier().setEnabled(enabled);

				loadPolicy(getPreloadedPolicies().get(PreferencesPanel.this.getPolicies().getSelectedIndex()));
			}
		});

		final boolean enableTextFields = this.policies.getSelectedIndex() == (this.policies.getItemCount()-1);

		this.policyIdentifier.setEnabled(enableTextFields);
		final JLabel policyIdentifierLabel = new JLabel("Identificador de la pol�tica (URI)");
		policyIdentifierLabel.setLabelFor(this.policyIdentifier);
		panel.add(policyIdentifierLabel);
		panel.add(this.policyIdentifier);

		this.policyIdentifierHash.setEnabled(enableTextFields);
		final JLabel policyIdentifierHashLabel = new JLabel("Huella digital del identificador de la pol�tica (en Base64)");
		policyIdentifierHashLabel.setLabelFor(this.policyIdentifierHash);
		panel.add(policyIdentifierHashLabel);
		panel.add(this.policyIdentifierHash);

		this.policyIdentifierHashAlgorithm.setEnabled(enableTextFields);
		final JLabel policyIdentifierHashAlgorithmLabel = new JLabel("Algoritmo de la huella digital del identificador de la pol�tica");
		policyIdentifierHashAlgorithmLabel.setLabelFor(this.policyIdentifierHashAlgorithm);
		panel.add(policyIdentifierHashAlgorithmLabel);
		panel.add(this.policyIdentifierHashAlgorithm);

		this.policyQualifier.setEnabled(enableTextFields);
		final JLabel policyQualifierLabel = new JLabel("Calificador de la pol�tica (URL)");
		policyQualifierLabel.setLabelFor(this.policyQualifier);
		panel.add(policyQualifierLabel);
		panel.add(this.policyQualifier);

		// Cargamos la politica de las preferencias cambiado el Combo si es preciso
		final AdESPolicy savedPolicy = preloadedPolicies.get(this.policies.getItemCount()-1);
		if (savedPolicy != null) {
			if (POLICY_AGE.equals(savedPolicy)) {
				this.policies.setSelectedIndex(POLICY_INDEX_AGE);
			}
			else {
				this.policies.setSelectedIndex(this.policies.getItemCount()-1);
			}
		}

		return panel;
	}

	private JPanel createButtonsPanel() {
		final JPanel panel = new JPanel();
		final JButton cancelButton = new JButton("Cancelar");
		cancelButton.setMnemonic('C');
		cancelButton.getAccessibleContext().setAccessibleDescription(
			"Cancela el establecimiento de preferencias descartando los cambios realizados desde la �ltima vez que se guardaron"
		);
		panel.add(cancelButton);
		final JButton acceptButton = new JButton("Aceptar");
		acceptButton.setMnemonic('A');
		acceptButton.getAccessibleContext().setAccessibleDescription(
			"Guarda los valores establecidos para las preferencias y finaliza el proceso de establecimiento de estas cerrando la ventana"
		);
		acceptButton.addActionListener(new ActionListener() {
			/** {@inheritDoc} */
			@Override
			public void actionPerformed(final ActionEvent ae) {
				savePreferences();
			}
		});
		panel.add(acceptButton);
		final JButton applyButton = new JButton("Aplicar");
		applyButton.setMnemonic('p');
		applyButton.getAccessibleContext().setAccessibleDescription(
			"Guarda los valores establecidos para las preferencias permitiendo continuar con el proceso de establecimiento de estas"
		);
		applyButton.addActionListener(new ActionListener() {
			/** {@inheritDoc} */
			@Override
			public void actionPerformed(final ActionEvent ae) {
				savePreferences();
			}
		});
		panel.add(applyButton);
		return panel;
	}

	PreferencesPanel() {
		SwingUtilities.invokeLater(new Runnable() {
			/** {@inheritDoc} */
			@Override
			public void run() {
				createUI();
			}
		});
	}

	void loadPolicy(final AdESPolicy policy) {
		if (policy != null) {
			PreferencesPanel.this.getPolicyIdentifier().setText(policy.getPolicyIdentifier());
			PreferencesPanel.this.getPolicyIdentifierHash().setText(policy.getPolicyIdentifierHash());
			PreferencesPanel.this.getPolicyIdentifierHashAlgorithm().setSelectedItem(policy.getPolicyIdentifierHashAlgorithm());
			PreferencesPanel.this.getPolicyQualifier().setText(policy.getPolicyQualifier().toString());
		}
		else {
			PreferencesPanel.this.getPolicyIdentifier().setText(""); //$NON-NLS-1$
			PreferencesPanel.this.getPolicyIdentifierHash().setText(""); //$NON-NLS-1$
			PreferencesPanel.this.getPolicyQualifier().setText(""); //$NON-NLS-1$
		}
	}








public static void main(final String args[]) {
	final javax.swing.JFrame frame = new javax.swing.JFrame();
	frame.add(new PreferencesPanel());
	frame.setBounds(50,50,800,600);
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setVisible(true);
}


}