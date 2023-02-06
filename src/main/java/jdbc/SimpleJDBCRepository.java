package jdbc;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleJDBCRepository {

    private Connection connection = null;
    private PreparedStatement ps = null;
    private Statement st = null;
    private CustomDataSource dataSource;

    {
        try {
            dataSource = CustomDataSource.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static final String CREATE_USER_SQL = """
            INSERT INTO myusers(
            firstname, lastname, age)
            VALUES (?, ?, ?);
            """;
    private static final String UPDATE_USER_SQL = """
            UPDATE myusers
            SET firstname=?, lastname=?, age=?
            WHERE id = ?
            """;
    private static final String DELETE_USER = """
            DELETE FROM public.myusers
            WHERE id = ?
            """;
    private static final String FIND_USER_BY_ID_SQL = """
            SELECT id, firstname, lastname, age FROM myusers
            WHERE id = ?
            """;
    private static final String FIND_USER_BY_NAME_SQL = """
            SELECT id, firstname, lastname, age FROM myusers
            WHERE firstname LIKE CONCAT('%', ?, '%')
            """;
    private static final String FIND_ALL_USER_SQL = """
            SELECT id, firstname, lastname, age FROM myusers
            """;

    public Long createUser(User user) throws IOException {
        Long id = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(CREATE_USER_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setObject(1, user.getFirstName());
            statement.setObject(2, user.getLastName());
            statement.setObject(3, user.getAge());
            statement.execute();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                id = generatedKeys.getLong(1);
                ResultSet resultSet = statement.getGeneratedKeys();
                if (resultSet.next()) {
                    id = resultSet.getLong(1);
                }
            }
        } catch (SQLException e) {
                e.printStackTrace();
        }
        return id;
    }

    public User findUserById(Long userId) throws IOException {
        User user = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_ID_SQL)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                user = map(resultSet);
            }
        } catch (SQLException e) {
        e.printStackTrace();
        }
        return user;
    }

    public User findUserByName(String userName) throws IOException {
        User user = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_NAME_SQL)) {
            statement.setString(1, userName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                user = map(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    public List<User> findAllUser() throws IOException {
        List<User> users = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_USER_SQL)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                users.add(map(resultSet));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public User updateUser( User user) throws IOException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_USER_SQL)) {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setInt(3, user.getAge());
            statement.setLong(4, user.getId());
            if (statement.executeUpdate() != 0) {
                return findUserById(user.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteUser(Long userId) throws IOException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_USER)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User map(ResultSet rs) throws SQLException {

        return User.builder()
            .id(rs.getLong("id"))
            .firstName(rs.getString("firstname"))
            .lastName(rs.getString("lastname"))
            .age(rs.getInt("age"))
            .build();

    }
}
