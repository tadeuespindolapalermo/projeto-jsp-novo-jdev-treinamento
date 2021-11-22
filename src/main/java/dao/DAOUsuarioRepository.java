package dao;

import static java.util.Objects.nonNull;
import static util.ObjectUtil.isObjectValid;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import connection.SingleConnection;
import model.ModelLogin;

public class DAOUsuarioRepository implements Serializable {
	
	private static final long serialVersionUID = -6165831396485571353L;
	
	private transient Connection connection;
	
	public DAOUsuarioRepository() {
		connection = SingleConnection.getConnection();
	}
	
	public ModelLogin gravarUsuario(ModelLogin objeto, Long usuarioLogado) throws SQLException {
		var index = 0;
		var sql = objeto.isNovo() ?
			"INSERT INTO model_login (rendamensal, datanascimento, cep, logradouro, bairro, localidade, uf, numero, sexo, perfil, login, senha, nome, email, usuario_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
			"UPDATE model_login SET rendamensal = ?, datanascimento = ?, cep=?, logradouro=?, bairro=?, localidade=?, uf=?, numero=?, sexo=?, perfil=?, login=?, senha=?, nome=?, email=? WHERE id=?";
		try (PreparedStatement preparedInsert = connection.prepareStatement(sql)) {
			preparedInsert.setDouble(++ index, objeto.getRendaMensal());
			preparedInsert.setDate(++ index, objeto.getDataNascimento());
			preparedInsert.setString(++ index, objeto.getEndereco().getCep());
			preparedInsert.setString(++ index, objeto.getEndereco().getLogradouro());
			preparedInsert.setString(++ index, objeto.getEndereco().getBairro());
			preparedInsert.setString(++ index, objeto.getEndereco().getLocalidade());
			preparedInsert.setString(++ index, objeto.getEndereco().getUf());
			preparedInsert.setString(++ index, objeto.getEndereco().getNumero());
			preparedInsert.setString(++ index, objeto.getSexo());
			preparedInsert.setString(++ index, objeto.getPerfil());
			preparedInsert.setString(++ index, objeto.getLogin());
			preparedInsert.setString(++ index, objeto.getSenha());
			preparedInsert.setString(++ index, objeto.getNome());
			preparedInsert.setString(++ index, objeto.getEmail());
			preparedInsert.setLong(++ index, objeto.isNovo() ? usuarioLogado : objeto.getId());
			preparedInsert.executeUpdate();
			connection.commit();
			if (isObjectValid(objeto.getFoto())) {
				var where = objeto.isNovo() ? "login" : "id";
				sql = "UPDATE model_login SET foto=?, extensaoFoto=? WHERE " + where + "=?";
				try(PreparedStatement preparedUpdate = connection.prepareStatement(sql)) {
					preparedUpdate.setString(1, objeto.getFoto());
					preparedUpdate.setString(2, objeto.getExtensaoFoto());
					if (objeto.isNovo()) {
						preparedUpdate.setString(3, objeto.getLogin());
					} else {
						preparedUpdate.setLong(3, objeto.getId());
					}
					preparedUpdate.executeUpdate();
					connection.commit();
				}
			}
		}
		return buscarUsuario(objeto.getLogin(), usuarioLogado);
	}
	
	public List<ModelLogin> listarTodosSemLimit(Long usuarioLogado) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE useradmin IS FALSE AND usuario_id = ?";
		return consultar(sql, false, false, false, usuarioLogado);
	}
	
	public List<ModelLogin> listarTodos(Long usuarioLogado) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE useradmin IS FALSE AND usuario_id = ? LIMIT 5";
		return consultar(sql, false, false, false, usuarioLogado);
	}
	
	public List<ModelLogin> listarTodosPaginado(Long usuarioLogado, Integer offset) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE useradmin IS FALSE AND usuario_id = ? ORDER BY nome OFFSET " + offset + " LIMIT 5";
		return consultar(sql, false, false, false, usuarioLogado);
	}
	
	public List<ModelLogin> buscarPorNome(String nome, Long usuarioLogado) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE UPPER(nome) LIKE UPPER(?) AND useradmin IS FALSE AND usuario_id = ? LIMIT 5";
		return consultar(sql, false, false, false, "%" + nome + "%", usuarioLogado);
	}
	
	public List<ModelLogin> buscarPorNomePaginado(String nome, Long usuarioLogado, String offset) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE UPPER(nome) LIKE UPPER(?) AND useradmin IS FALSE AND usuario_id=? OFFSET " + offset + " LIMIT 5";
		return consultar(sql, false, false, false, "%" + nome + "%", usuarioLogado);
	}
	
	public ModelLogin buscarPoId(String id, Long usuarioLogado) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE id = ? AND useradmin IS FALSE AND usuario_id = ?";
		return consultar(sql, true, true, true, Long.parseLong(id), usuarioLogado).stream().findFirst().orElse(new ModelLogin());
	}
	
	public ModelLogin buscarPoId(Long id) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE id = ? AND useradmin IS FALSE";
		return consultar(sql, true, true, true, id).stream().findFirst().orElse(new ModelLogin());
	}
	
	public ModelLogin buscarUsuario(String login, Long usuarioLogado) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE UPPER(login) = UPPER(?) AND useradmin IS FALSE AND usuario_id = ?";
		return consultar(sql, true, true, true, login, usuarioLogado).stream().findFirst().orElse(new ModelLogin());
	}
	
	public ModelLogin buscarUsuario(String login) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE UPPER(login) = UPPER(?) AND useradmin IS FALSE";
		return consultar(sql, true, true, true, login).stream().findFirst().orElse(new ModelLogin());
	}
	
	public ModelLogin buscarUsuarioLogado(String login) throws SQLException {
		var sql = "SELECT * FROM model_login WHERE UPPER(login) = UPPER(?)";
		return consultar(sql, true, true, true, login).stream().findFirst().orElse(new ModelLogin());
	}
	
	private List<ModelLogin> consultar(String sql, boolean renderPassword, boolean renderFoto, boolean renderEndereco, Object... parametros) throws SQLException {
		List<ModelLogin> retorno = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			if (nonNull(parametros)) {
				if (parametros[0] instanceof String parameter) statement.setString(1, parameter);
				if (parametros[0] instanceof Long parameter) statement.setLong(1, parameter);
				if (parametros.length > 1 && parametros[1] instanceof Long parameter) statement.setLong(2, parameter);
			}
			ResultSet resultado = statement.executeQuery();
			while (resultado.next()) {
				var modelLogin = new ModelLogin(
					resultado.getLong("id"),
					resultado.getString("nome"),
					resultado.getString("email"),
					resultado.getString("login"),					
					renderPassword ? resultado.getString("senha") : null,
					resultado.getBoolean("useradmin"),
					resultado.getString("perfil"),
					resultado.getString("sexo"),
					renderFoto ? resultado.getString("foto") : null,
					renderFoto ? resultado.getString("extensaoFoto") : null,
					resultado.getDate("datanascimento"),
					resultado.getDouble("rendamensal")
				);	
				if (renderEndereco) preencherEndereco(modelLogin, resultado);
				retorno.add(modelLogin);
			}
		}
		return retorno;
	}
	
	private void preencherEndereco(ModelLogin modelLogin, ResultSet resultado) throws SQLException {
		modelLogin.getEndereco().setCep(resultado.getString("cep"));
		modelLogin.getEndereco().setLogradouro(resultado.getString("logradouro"));
		modelLogin.getEndereco().setBairro(resultado.getString("bairro"));
		modelLogin.getEndereco().setLocalidade(resultado.getString("localidade"));
		modelLogin.getEndereco().setUf(resultado.getString("uf"));
		modelLogin.getEndereco().setNumero(resultado.getString("numero"));
	}
	
	public boolean validarLogin(String login) throws SQLException {
		var sql = "SELECT COUNT(1) > 0 AS existe FROM model_login WHERE UPPER(login) = UPPER(?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, login);
			ResultSet resultado = statement.executeQuery();
			resultado.next();
			return resultado.getBoolean("existe");
		}
	}
	
	public void deletarUsuario(String id) throws SQLException {
		var sql = "DELETE FROM model_login WHERE id = ? AND useradmin IS FALSE";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, Long.parseLong(id));
			statement.executeUpdate();
			connection.commit();
		}
	}
	
	public int getTotalPaginas(Long usuarioLogado) throws SQLException {
		var sql = "SELECT COUNT(1) AS total FROM model_login WHERE usuario_id =?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, usuarioLogado);
			return getCalculoTotalPaginas(statement);
		}
	}
	
	public int getTotalPaginasPorNome(String nome, Long usuarioLogado) throws SQLException {
		var sql = "SELECT COUNT(1) AS total FROM model_login WHERE UPPER(nome) LIKE UPPER(?) AND useradmin IS FALSE AND usuario_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, "%" + nome + "%");
			statement.setLong(2, usuarioLogado);
			return getCalculoTotalPaginas(statement);
		}
	}
	
	private int getCalculoTotalPaginas(PreparedStatement statement) throws SQLException {
		ResultSet resultado = statement.executeQuery();
		resultado.next();
		Double cadastros = resultado.getDouble("total");
		Double registrosPorPagina = 5.0;
		Double pagina = cadastros / registrosPorPagina;
		Double resto = pagina % 2;
		if (resto > 0) pagina ++;
		return pagina.intValue();
	}

}
